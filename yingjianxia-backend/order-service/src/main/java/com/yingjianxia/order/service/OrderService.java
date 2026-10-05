package com.yingjianxia.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yingjianxia.order.dto.CartAddReq;
import com.yingjianxia.order.dto.CartUpdateReq;
import com.yingjianxia.order.dto.OrderCreateReq;
import com.yingjianxia.order.dto.OrderQueryReq;
import com.yingjianxia.order.entity.CartItem;
import com.yingjianxia.order.entity.Order;
import com.yingjianxia.order.entity.OrderStatusLog;

import java.util.List;

/**
 * 订单服务接口
 * <p>
 * 核心：幂等下单 / 商品快照 / TCC库存预扣 / 价格计算 / 状态机流转 / 自动取消确认 / 双角色视图
 */
public interface OrderService {

    /* ========== 下单 & 状态流转（买家端） ========== */
    /** 下单：幂等键校验 → 商品快照 → 库存预扣RPC → 价格计算 → 优惠券/积分抵扣 → 写订单+明细 → 自动取消截止时间 */
    Order createOrder(OrderCreateReq req, Long buyerId);

    /** 买家取消订单（仅待付款/待发货可取消，触发担保退款回写） */
    void cancelOrder(Long orderId, Long userId, String reason);

    /** 买家确认收货（待收货 → 已完成，触发担保放款回调） */
    void confirmReceive(Long orderId, Long userId);

    /* ========== 卖家端 ========== */
    /** 卖家发货（待发货 → 待收货，设置自动确认截止时间） */
    void shipOrder(Long orderId, Long sellerId);

    /** 买家提醒发货（推送站内信给卖家） */
    void remindShip(Long orderId, Long buyerId);

    /* ========== 查询（买家/卖家/管理端） ========== */
    /** 订单详情（含明细） */
    Order getOrderDetail(Long orderId);

    /** 买家订单列表 */
    IPage<Order> listBuyerOrders(Long buyerId, OrderQueryReq req);

    /** 卖家订单列表 */
    IPage<Order> listSellerOrders(Long sellerId, OrderQueryReq req);

    /** 管理端订单列表 */
    IPage<Order> listOrders(OrderQueryReq req);

    /** 订单状态变更日志 */
    List<OrderStatusLog> getStatusLogs(Long orderId);

    /* ========== 购物车 CRUD ========== */
    Long addCart(CartAddReq req, Long userId);

    void updateCart(CartUpdateReq req, Long userId);

    List<CartItem> listCart(Long userId);

    void removeCart(Long cartItemId, Long userId);

    void clearSelectedCart(Long userId);

    /* ========== [内部] 其他服务回调 ========== */
    /** TCC 库存回写（Confirm/Cancel 阶段同步订单发货状态） */
    void tccStockCallback(Long orderId, int phase, boolean success, String reason);

    /** 担保/支付回调：标记订单已付款（待付款 → 待发货） */
    void markPaid(Long orderId, String paymentMethod, String idempotencyKey);

    /** 状态变更（内部，带操作人类型） */
    void changeStatus(Long orderId, int toStatus, Long operatorId, int operatorType, String remark);
}
