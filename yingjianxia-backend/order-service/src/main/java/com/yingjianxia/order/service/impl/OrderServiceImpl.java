package com.yingjianxia.order.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.order.dto.CartAddReq;
import com.yingjianxia.order.dto.CartUpdateReq;
import com.yingjianxia.order.dto.OrderCreateReq;
import com.yingjianxia.order.dto.OrderQueryReq;
import com.yingjianxia.order.entity.CartItem;
import com.yingjianxia.order.entity.Order;
import com.yingjianxia.order.entity.OrderItem;
import com.yingjianxia.order.entity.OrderStatusLog;
import com.yingjianxia.order.enums.OrderErrorCode;
import com.yingjianxia.order.feign.ProductFeignClient;
import com.yingjianxia.order.mapper.CartItemMapper;
import com.yingjianxia.order.mapper.OrderItemMapper;
import com.yingjianxia.order.mapper.OrderMapper;
import com.yingjianxia.order.mapper.OrderStatusLogMapper;
import com.yingjianxia.order.service.OrderService;
import com.yingjianxia.common.core.result.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 * <p>
 * 核心：幂等下单 + 商品快照 + 库存预扣 + 价格计算 + 状态机流转 + 自动取消确认 + 双角色视图
 * <p>
 * 跨服务 RPC（product-service 库存/快照、escrow-service 担保冻结/放款、marketing-service 优惠券）
 * 当前以占位方法 + TODO 标注，待对应服务就绪后接入 Feign。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final CartItemMapper cartItemMapper;
    private final ObjectMapper objectMapper;
    private final ProductFeignClient productFeignClient;

    @Value("${yingjianxia.order.auto-cancel-minutes:30}")
    private int autoCancelMinutes;

    @Value("${yingjianxia.order.auto-confirm-hours:168}")
    private int autoConfirmHours;

    /* ============================ 下单 ============================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(OrderCreateReq req, Long buyerId) {
        // 1. 幂等键校验（防重复下单）
        if (StrUtil.isNotBlank(req.getIdempotencyKey())) {
            // 长度校验（bug-20260908210500）：超长直接返回业务错误，避免数据库抛 304
            if (req.getIdempotencyKey().length() > 128) {
                throw new BusinessException(OrderErrorCode.IDEMPOTENT_KEY_TOO_LONG);
            }
            Long dup = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .eq(Order::getIdempotencyKey, req.getIdempotencyKey()));
            if (dup != null && dup > 0) {
                throw new BusinessException(OrderErrorCode.DUPLICATE_ORDER);
            }
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BusinessException(OrderErrorCode.ORDER_ITEM_EMPTY);
        }
        if (req.getAddressId() == null) {
            throw new BusinessException(OrderErrorCode.ADDRESS_REQUIRED);
        }

        // 2. 多卖家拆单：当前实现按首个 sellerId 聚合为单订单（多店铺拆单由上层编排）
        Long sellerId = req.getItems().get(0).getSellerId();
        if (sellerId == null) {
            sellerId = fetchSellerId(req.getItems().get(0).getProductId());
        }

        // 3. 计算总额 + 生成明细（商品快照 + 库存预扣 RPC）
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderCreateReq.OrderItemReq it : req.getItems()) {
            if (it.getQuantity() == null || it.getQuantity() <= 0) {
                throw new BusinessException(OrderErrorCode.QUANTITY_INVALID);
            }
            // TODO: RPC product-service 预扣库存（TCC-Try），失败抛 STOCK_DEDUCT_FAIL
            boolean stockOk = deductStockPlaceholder(it.getProductId(), it.getQuantity());
            if (!stockOk) {
                throw new BusinessException(OrderErrorCode.INSUFFICIENT_STOCK);
            }
            ProductSnapshot snap = fetchProductSnapshot(it.getProductId());
            BigDecimal subtotal = snap.getPrice().multiply(BigDecimal.valueOf(it.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
        }

        // 4. 优惠券 / 积分抵扣
        BigDecimal discount = BigDecimal.ZERO;
        if (req.getCouponId() != null) {
            // TODO: RPC marketing-service 校验优惠券并返回抵扣金额
            discount = discount.add(validateCouponPlaceholder(req.getCouponId(), totalAmount));
        }
        if (req.getPointsUsed() != null && req.getPointsUsed() > 0) {
            // TODO: RPC user-service 校验积分余额；按 100积分=1元 抵扣
            BigDecimal pointsDiscount = BigDecimal.valueOf(req.getPointsUsed()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            discount = discount.add(pointsDiscount);
        }
        if (discount.compareTo(totalAmount) > 0) {
            throw new BusinessException(OrderErrorCode.COUPON_INVALID);
        }

        BigDecimal shippingFee = BigDecimal.ZERO; // TODO: 物流域运费计算
        BigDecimal payable = totalAmount.add(shippingFee).subtract(discount);

        // 5. 写订单主表
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setBuyerId(buyerId);
        order.setSellerId(sellerId);
        order.setAddressId(req.getAddressId());
        order.setAddressSnapshot(StrUtil.isBlank(req.getAddressSnapshot()) ? null : req.getAddressSnapshot());
        order.setTotalAmount(totalAmount);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(discount);
        order.setPayableAmount(payable);
        order.setCouponId(req.getCouponId());
        order.setPointsUsed(req.getPointsUsed() == null ? 0 : req.getPointsUsed());
        order.setStatus(Order.STATUS_PENDING_PAY);
        order.setPaymentMethod(req.getPaymentMethod());
        order.setRemark(req.getRemark());
        order.setIdempotencyKey(req.getIdempotencyKey());
        order.setAutoCancelDeadline(now.plusMinutes(autoCancelMinutes));
        orderMapper.insert(order);

        // 6. 写订单明细
        for (OrderCreateReq.OrderItemReq it : req.getItems()) {
            ProductSnapshot snap = fetchProductSnapshot(it.getProductId());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(it.getProductId());
            item.setProductSnapshot(toSnapshotJson(snap));
            item.setUnitPrice(snap.getPrice());
            item.setQuantity(it.getQuantity());
            item.setSubtotal(snap.getPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
            item.setShipmentStatus(OrderItem.SHIP_NOT_SHIPPED);
            orderItemMapper.insert(item);
        }

        // 7. 状态变更日志（NULL → 待付款）
        recordStatusLog(order.getId(), null, Order.STATUS_PENDING_PAY, buyerId, Order.OP_BUYER, "买家下单");

        // 8. 清空购物车已下单项（按商品ID）
        // TODO: 触发 escrow-service 担保冻结（下单时冻结买家资金）
        log.info("【下单成功】orderNo={}, buyerId={}, payable={}", order.getOrderNo(), buyerId, payable);
        return getOrderDetail(order.getId());
    }

    /* ============================ 状态流转 ============================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId, String reason) {
        Order order = requireOrder(orderId);
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(OrderErrorCode.NOT_BUYER_OF_ORDER);
        }
        int st = order.getStatus();
        if (st != Order.STATUS_PENDING_PAY && st != Order.STATUS_PENDING_SHIP) {
            throw new BusinessException(OrderErrorCode.CANCEL_NOT_ALLOWED);
        }
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .set(Order::getStatus, Order.STATUS_CANCELLED)
                .set(Order::getCancelReason, reason)
                .set(Order::getUpdatedAt, LocalDateTime.now()));
        recordStatusLog(orderId, st, Order.STATUS_CANCELLED, userId, Order.OP_BUYER,
                StrUtil.isBlank(reason) ? "买家取消" : reason);
        // TODO: RPC escrow-service 退款（解冻买家资金）+ product-service 回写库存
        log.info("【订单取消】orderId={}, buyerId={}, reason={}", orderId, userId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(Long orderId, Long userId) {
        Order order = requireOrder(orderId);
        if (!order.getBuyerId().equals(userId)) {
            throw new BusinessException(OrderErrorCode.NOT_BUYER_OF_ORDER);
        }
        if (order.getStatus() != Order.STATUS_PENDING_RECEIVE) {
            throw new BusinessException(OrderErrorCode.CONFIRM_NOT_ALLOWED);
        }
        LocalDateTime now = LocalDateTime.now();
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .set(Order::getStatus, Order.STATUS_COMPLETED)
                .set(Order::getReceivedAt, now)
                .set(Order::getUpdatedAt, now));
        // 明细发货状态 → 已签收
        orderItemMapper.update(null, new LambdaUpdateWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .set(OrderItem::getShipmentStatus, OrderItem.SHIP_SIGNED));
        recordStatusLog(orderId, Order.STATUS_PENDING_RECEIVE, Order.STATUS_COMPLETED, userId, Order.OP_BUYER, "买家确认收货");
        // TODO: RPC escrow-service 担保放款（确认收货时放款给卖家，扣除手续费）
        log.info("【确认收货】orderId={}, buyerId={}", orderId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long orderId, Long sellerId) {
        Order order = requireOrder(orderId);
        if (!order.getSellerId().equals(sellerId)) {
            throw new BusinessException(OrderErrorCode.NOT_SELLER_OF_ORDER);
        }
        if (order.getStatus() != Order.STATUS_PENDING_SHIP) {
            throw new BusinessException(OrderErrorCode.SHIP_NOT_ALLOWED);
        }
        LocalDateTime now = LocalDateTime.now();
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .set(Order::getStatus, Order.STATUS_PENDING_RECEIVE)
                .set(Order::getShippedAt, now)
                .set(Order::getAutoConfirmDeadline, now.plusHours(autoConfirmHours))
                .set(Order::getUpdatedAt, now));
        orderItemMapper.update(null, new LambdaUpdateWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .set(OrderItem::getShipmentStatus, OrderItem.SHIP_SHIPPED));
        recordStatusLog(orderId, Order.STATUS_PENDING_SHIP, Order.STATUS_PENDING_RECEIVE, sellerId, Order.OP_SELLER, "卖家发货");
        log.info("【卖家发货】orderId={}, sellerId={}", orderId, sellerId);
    }

    @Override
    public void remindShip(Long orderId, Long buyerId) {
        Order order = requireOrder(orderId);
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != Order.STATUS_PENDING_SHIP) {
            throw new BusinessException(OrderErrorCode.SHIP_NOT_ALLOWED);
        }
        // 记录提醒发货操作（实际可接入消息中心推送站内信给卖家）
        recordStatusLog(orderId, order.getStatus(), order.getStatus(), buyerId, Order.OP_BUYER, "买家提醒发货");
        log.info("【买家提醒发货】orderId={}, buyerId={}", orderId, buyerId);
    }

    /* ============================ 查询 ============================ */

    @Override
    public Order getOrderDetail(Long orderId) {
        Order order = requireOrder(orderId);
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        order.setItems(items);
        return order;
    }

    @Override
    public IPage<Order> listBuyerOrders(Long buyerId, OrderQueryReq req) {
        Page<Order> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<Order> w = buildWrapper(req).eq(Order::getBuyerId, buyerId)
                .orderByDesc(Order::getCreatedAt);
        IPage<Order> result = orderMapper.selectPage(page, w);
        fillOrderItems(result.getRecords());
        return result;
    }

    @Override
    public IPage<Order> listSellerOrders(Long sellerId, OrderQueryReq req) {
        Page<Order> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<Order> w = buildWrapper(req).eq(Order::getSellerId, sellerId)
                .orderByDesc(Order::getCreatedAt);
        IPage<Order> result = orderMapper.selectPage(page, w);
        fillOrderItems(result.getRecords());
        return result;
    }

    @Override
    public IPage<Order> listOrders(OrderQueryReq req) {
        Page<Order> page = new Page<>(req.getPageNum(), req.getPageSize());
        LambdaQueryWrapper<Order> w = buildWrapper(req).orderByDesc(Order::getCreatedAt);
        IPage<Order> result = orderMapper.selectPage(page, w);
        fillOrderItems(result.getRecords());
        return result;
    }

    /** 批量查询订单明细并组装到每个订单（避免 N+1 查询） */
    private void fillOrderItems(List<Order> orders) {
        if (orders == null || orders.isEmpty()) return;
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItem> allItems = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderId, orderIds));
        Map<Long, List<OrderItem>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        orders.forEach(o -> o.setItems(itemMap.getOrDefault(o.getId(), Collections.emptyList())));
    }

    @Override
    public List<OrderStatusLog> getStatusLogs(Long orderId) {
        return statusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, orderId)
                .orderByAsc(OrderStatusLog::getId));
    }

    /* ============================ 购物车 ============================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addCart(CartAddReq req, Long userId) {
        CartItem exist = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, req.getProductId()));
        if (exist != null) {
            cartItemMapper.update(null, new LambdaUpdateWrapper<CartItem>()
                    .eq(CartItem::getId, exist.getId())
                    .setSql("quantity = quantity + " + req.getQuantity())
                    .set(CartItem::getIsSelected, req.getIsSelected() == null ? CartItem.SELECTED_YES : req.getIsSelected())
                    .set(CartItem::getUpdatedAt, LocalDateTime.now()));
            return exist.getId();
        }
        CartItem c = new CartItem();
        c.setUserId(userId);
        c.setProductId(req.getProductId());
        c.setQuantity(req.getQuantity());
        c.setIsSelected(req.getIsSelected() == null ? CartItem.SELECTED_YES : req.getIsSelected());
        try {
            cartItemMapper.insert(c);
        } catch (org.springframework.dao.DuplicateKeyException dup) {
            // 唯一约束冲突：说明存在已逻辑删除的同款商品，恢复它
            // 注意：@TableLogic 字段不能用 MyBatis-Plus Wrapper 更新（WHERE 会自动加 deleted=0），
            // 必须用自定义 SQL 绕过逻辑删除过滤
            int restored = cartItemMapper.restoreDeletedCartItem(
                    userId, req.getProductId(), req.getQuantity(),
                    req.getIsSelected() == null ? CartItem.SELECTED_YES : req.getIsSelected());
            if (restored > 0) {
                // 重新查询获取恢复后的ID
                CartItem item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .eq(CartItem::getProductId, req.getProductId()));
                return item != null ? item.getId() : c.getId();
            }
            throw dup;
        }
        return c.getId();
    }

    @Override
    public void updateCart(CartUpdateReq req, Long userId) {
        CartItem c = requireCart(req.getId(), userId);
        LambdaUpdateWrapper<CartItem> u = new LambdaUpdateWrapper<CartItem>()
                .eq(CartItem::getId, req.getId())
                .set(CartItem::getUpdatedAt, LocalDateTime.now());
        if (req.getQuantity() != null) u.set(CartItem::getQuantity, req.getQuantity());
        if (req.getIsSelected() != null) u.set(CartItem::getIsSelected, req.getIsSelected());
        cartItemMapper.update(null, u);
    }

    @Override
    public List<CartItem> listCart(Long userId) {
        return cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId).orderByDesc(CartItem::getCreatedAt));
    }

    @Override
    public void removeCart(Long cartItemId, Long userId) {
        requireCart(cartItemId, userId);
        cartItemMapper.deleteById(cartItemId);
    }

    @Override
    public void clearSelectedCart(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getIsSelected, CartItem.SELECTED_YES));
    }

    /* ============================ [内部] 回调 ============================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void tccStockCallback(Long orderId, int phase, boolean success, String reason) {
        // TCC phase: 1=Try 2=Confirm 3=Cancel；失败时记录并触发取消
        log.info("【TCC库存回写】orderId={}, phase={}, success={}, reason={}", orderId, phase, success, reason);
        if (!success) {
            Order order = orderMapper.selectById(orderId);
            if (order != null && order.getStatus() == Order.STATUS_PENDING_PAY) {
                changeStatusInternal(order, Order.STATUS_CANCELLED, null, Order.OP_SYSTEM, "库存预扣失败自动取消:" + reason);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Long orderId, String paymentMethod, String idempotencyKey) {
        Order order = requireOrder(orderId);
        if (order.getStatus() != Order.STATUS_PENDING_PAY) {
            throw new BusinessException(OrderErrorCode.ORDER_ALREADY_PAID);
        }
        LocalDateTime now = LocalDateTime.now();
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .set(Order::getStatus, Order.STATUS_PENDING_SHIP)
                .set(Order::getPaymentMethod, paymentMethod)
                .set(Order::getPaidAt, now)
                .set(Order::getAutoCancelDeadline, null) // 已付款，取消自动取消
                .set(Order::getUpdatedAt, now));
        recordStatusLog(orderId, Order.STATUS_PENDING_PAY, Order.STATUS_PENDING_SHIP, null, Order.OP_SYSTEM, "支付成功");
        log.info("【订单支付成功】orderId={}, method={}", orderId, paymentMethod);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long orderId, int toStatus, Long operatorId, int operatorType, String remark) {
        Order order = requireOrder(orderId);
        validateTransition(order.getStatus(), toStatus);
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .set(Order::getStatus, toStatus)
                .set(Order::getUpdatedAt, LocalDateTime.now()));
        recordStatusLog(orderId, order.getStatus(), toStatus, operatorId, operatorType, remark);
    }

    /* ============================ 内部工具 ============================ */

    private Order requireOrder(Long id) {
        Order o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException(OrderErrorCode.ORDER_NOT_FOUND);
        return o;
    }

    private CartItem requireCart(Long id, Long userId) {
        CartItem c = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getId, id).eq(CartItem::getUserId, userId));
        if (c == null) throw new BusinessException(OrderErrorCode.CART_ITEM_NOT_FOUND);
        return c;
    }

    private LambdaQueryWrapper<Order> buildWrapper(OrderQueryReq req) {
        LambdaQueryWrapper<Order> w = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(req.getOrderNo())) w.eq(Order::getOrderNo, req.getOrderNo());
        if (req.getStatus() != null) w.eq(Order::getStatus, req.getStatus());
        if (req.getBuyerId() != null) w.eq(Order::getBuyerId, req.getBuyerId());
        if (req.getSellerId() != null) w.eq(Order::getSellerId, req.getSellerId());
        return w;
    }

    /** 状态机校验：合法流转白名单 */
    private void validateTransition(Integer from, int to) {
        boolean ok = switch (from) {
            case Order.STATUS_PENDING_PAY -> to == Order.STATUS_PENDING_SHIP || to == Order.STATUS_CANCELLED;
            case Order.STATUS_PENDING_SHIP -> to == Order.STATUS_PENDING_RECEIVE || to == Order.STATUS_CANCELLED;
            case Order.STATUS_PENDING_RECEIVE -> to == Order.STATUS_COMPLETED || to == Order.STATUS_AFTERSALE;
            case Order.STATUS_COMPLETED -> to == Order.STATUS_AFTERSALE;
            case Order.STATUS_AFTERSALE -> to == Order.STATUS_COMPLETED || to == Order.STATUS_CANCELLED;
            default -> false;
        };
        if (!ok) throw new BusinessException(OrderErrorCode.INVALID_STATUS_TRANSITION);
    }

    private void changeStatusInternal(Order order, int toStatus, Long operatorId, int operatorType, String remark) {
        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .set(Order::getStatus, toStatus)
                .set(Order::getUpdatedAt, LocalDateTime.now()));
        recordStatusLog(order.getId(), order.getStatus(), toStatus, operatorId, operatorType, remark);
    }

    private void recordStatusLog(Long orderId, Integer from, int to, Long operatorId, int operatorType, String remark) {
        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(orderId);
        statusLog.setFromStatus(from);
        statusLog.setToStatus(to);
        statusLog.setOperatorId(operatorId);
        statusLog.setOperatorType(operatorType);
        statusLog.setRemark(remark);
        statusLogMapper.insert(statusLog);
    }

    private String generateOrderNo() {
        return "YJX" + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }

    /** 商品快照 JSON：开发期占位，生产应 RPC product-service 获取 */
    private String toSnapshotJson(ProductSnapshot snap) {
        try {
            return objectMapper.writeValueAsString(snap);
        } catch (Exception e) {
            return "{\"productId\":" + snap.getProductId() + "}";
        }
    }

    /* ---------- 跨服务 RPC：Feign 调用 product-service ---------- */

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class ProductSnapshot {
        private Long productId;
        private String title;
        private String image;
        private BigDecimal price;
        private Long sellerId;
    }

    /**
     * 通过 Feign 调用 product-service 获取商品快照信息
     */
    private ProductSnapshot fetchProductSnapshot(Long productId) {
        try {
            ApiResponse<Map<String, Object>> resp = productFeignClient.getProductDetail(productId);
            if (resp != null && resp.isSuccess() && resp.getData() != null) {
                Map<String, Object> data = resp.getData();
                ProductSnapshot snap = new ProductSnapshot();
                snap.setProductId(productId);
                snap.setTitle(data.get("title") != null ? String.valueOf(data.get("title")) : "未知商品");
                snap.setPrice(data.get("price") != null ? new BigDecimal(data.get("price").toString()) : BigDecimal.ZERO);
                snap.setSellerId(data.get("sellerId") != null ? Long.valueOf(data.get("sellerId").toString()) : 0L);
                // 取第一张图片
                Object images = data.get("imageUrls");
                if (images instanceof List<?> imgList && !imgList.isEmpty()) {
                    snap.setImage(String.valueOf(imgList.get(0)));
                } else {
                    snap.setImage("");
                }
                log.info("【Feign】获取商品快照成功 productId={}, title={}, price={}", productId, snap.getTitle(), snap.getPrice());
                return snap;
            } else {
                log.warn("【Feign】获取商品详情失败 productId={}, resp={}", productId, resp);
            }
        } catch (Exception e) {
            log.error("【Feign】调用 product-service 异常 productId={}", productId, e);
        }
        // 降级：返回默认值
        return new ProductSnapshot(productId, "商品获取失败", "", BigDecimal.ZERO, 0L);
    }

    /**
     * 通过 Feign 获取商品对应的卖家ID
     */
    private Long fetchSellerId(Long productId) {
        return fetchProductSnapshot(productId).getSellerId();
    }

    /**
     * 库存预扣（TCC-Try）— 当前仍为占位，后续接入 product-service TCC 接口
     */
    private boolean deductStockPlaceholder(Long productId, Integer quantity) {
        // TODO: Feign → product-service TCC-Try 预扣库存
        log.warn("【占位】库存预扣未实现 productId={}, quantity={}", productId, quantity);
        return true;
    }

    private BigDecimal validateCouponPlaceholder(Long couponId, BigDecimal totalAmount) {
        // TODO: Feign → marketing-service 校验优惠券并返回抵扣金额
        return BigDecimal.ZERO;
    }
}
