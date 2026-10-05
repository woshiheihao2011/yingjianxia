package com.yingjianxia.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.order.dto.CartAddReq;
import com.yingjianxia.order.dto.CartUpdateReq;
import com.yingjianxia.order.dto.OrderCreateReq;
import com.yingjianxia.order.dto.OrderQueryReq;
import com.yingjianxia.order.entity.CartItem;
import com.yingjianxia.order.entity.Order;
import com.yingjianxia.order.entity.OrderStatusLog;
import com.yingjianxia.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * 订单服务 Controller
 */
@Tag(name = "订单服务", description = "购物车/下单/状态流转/双角色视图/TCC库存回写")
@RestController
@RequestMapping({"/api/v1/order", "/api/v1/orders"})
@RequiredArgsConstructor
public class OrderController {
    @Value("${yingjianxia.internal-secret}")
    private String internalSecret;


    private final OrderService service;

    /* ========== 买家端 ========== */

    @Operation(summary = "[买家] 下单")
    @PostMapping({"", "/create"})
    public ApiResponse<Order> create(@Valid @RequestBody OrderCreateReq req) {
        return ApiResponse.success(service.createOrder(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "[买家] 取消订单")
    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long orderId,
                                    @RequestParam(required = false) String reason) {
        service.cancelOrder(orderId, UserContext.requiredUserId(), reason);
        return ApiResponse.success();
    }

    @Operation(summary = "[买家] 确认收货")
    @PostMapping("/{orderId}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long orderId) {
        service.confirmReceive(orderId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[买家] 提醒发货")
    @PostMapping("/{orderId}/remind-ship")
    public ApiResponse<Void> remindShip(@PathVariable Long orderId) {
        service.remindShip(orderId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[买家/卖家] 我的订单列表（role=seller 返回卖家订单）")
    @GetMapping({"", "/buyer/list"})
    public ApiResponse<IPage<Order>> buyerList(OrderQueryReq req) {
        Long userId = UserContext.requiredUserId();
        if ("seller".equalsIgnoreCase(req.getRole())) {
            return ApiResponse.success(service.listSellerOrders(userId, req));
        }
        return ApiResponse.success(service.listBuyerOrders(userId, req));
    }

    @Operation(summary = "[买家/卖家] 订单详情")
    @GetMapping("/{orderId}")
    public ApiResponse<Order> detail(@PathVariable Long orderId) {
        return ApiResponse.success(service.getOrderDetail(orderId));
    }

    @Operation(summary = "[买家/卖家] 订单状态变更日志")
    @GetMapping("/{orderId}/status-logs")
    public ApiResponse<List<OrderStatusLog>> statusLogs(@PathVariable Long orderId) {
        return ApiResponse.success(service.getStatusLogs(orderId));
    }

    /* ========== 卖家端 ========== */

    @Operation(summary = "[卖家] 发货")
    @PostMapping("/{orderId}/ship")
    public ApiResponse<Void> ship(@PathVariable Long orderId) {
        service.shipOrder(orderId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[卖家] 我的销售订单列表")
    @GetMapping("/seller/list")
    public ApiResponse<IPage<Order>> sellerList(OrderQueryReq req) {
        return ApiResponse.success(service.listSellerOrders(UserContext.requiredUserId(), req));
    }

    /* ========== 购物车 ========== */

    @Operation(summary = "[买家] 加购购物车")
    @PostMapping("/cart/add")
    public ApiResponse<Long> addCart(@Valid @RequestBody CartAddReq req) {
        return ApiResponse.success(service.addCart(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "[买家] 更新购物车")
    @PostMapping("/cart/update")
    public ApiResponse<Void> updateCart(@Valid @RequestBody CartUpdateReq req) {
        service.updateCart(req, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[买家] 购物车列表")
    @GetMapping("/cart/list")
    public ApiResponse<List<CartItem>> cartList() {
        return ApiResponse.success(service.listCart(UserContext.requiredUserId()));
    }

    @Operation(summary = "[买家] 删除购物车项")
    @DeleteMapping("/cart/{cartItemId}")
    public ApiResponse<Void> removeCart(@PathVariable Long cartItemId) {
        service.removeCart(cartItemId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "[买家] 清空已勾选购物车")
    @PostMapping("/cart/clear-selected")
    public ApiResponse<Void> clearSelected() {
        service.clearSelectedCart(UserContext.requiredUserId());
        return ApiResponse.success();
    }

    /* ========== [内部] 其他服务回调 ========== */

    @Operation(summary = "[内部] TCC库存回写", hidden = true)
    @PostMapping("/internal/{orderId}/tcc-stock")
    public ApiResponse<Void> tccStock(@PathVariable Long orderId,
                                      @RequestParam int phase,
                                      @RequestParam boolean success,
                                      @RequestParam(required = false) String reason,
                                      @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        service.tccStockCallback(orderId, phase, success, reason);
        return ApiResponse.success();
    }

    @Operation(summary = "[内部] 支付/担保回调标记已付款", hidden = true)
    @PostMapping("/internal/{orderId}/mark-paid")
    public ApiResponse<Void> markPaid(@PathVariable Long orderId,
                                      @RequestParam String paymentMethod,
                                      @RequestParam(required = false) String idempotencyKey,
                                      @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        service.markPaid(orderId, paymentMethod, idempotencyKey);
        return ApiResponse.success();
    }

    @Operation(summary = "[内部] 订单状态变更", hidden = true)
    @PostMapping("/internal/{orderId}/status")
    public ApiResponse<Void> changeStatus(@PathVariable Long orderId,
                                          @RequestParam int toStatus,
                                          @RequestParam(required = false) Long operatorId,
                                          @RequestParam int operatorType,
                                          @RequestParam(required = false) String remark,
                                          @RequestHeader("X-Internal-Secret") String secret) {
        if (!internalSecret.equals(secret)) return ApiResponse.fail(1, "auth fail");
        service.changeStatus(orderId, toStatus, operatorId, operatorType, remark);
        return ApiResponse.success();
    }

    /* ========== 管理端 ========== */

    @Operation(summary = "[管理端] 订单列表")
    @GetMapping("/admin/list")
    public ApiResponse<IPage<Order>> adminList(OrderQueryReq req) {
        return ApiResponse.success(service.listOrders(req));
    }
}
