package com.yingjianxia.order.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.order.dto.CartAddReq;
import com.yingjianxia.order.dto.CartUpdateReq;
import com.yingjianxia.order.entity.CartItem;
import com.yingjianxia.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车 Controller — 路径 /api/v1/cart（与前端对齐）
 *
 * @author 硬件侠后端团队
 */
@Tag(name = "购物车", description = "购物车增删改查")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final OrderService orderService;

    @Operation(summary = "购物车列表")
    @GetMapping
    public ApiResponse<List<CartItem>> list() {
        return ApiResponse.success(orderService.listCart(UserContext.requiredUserId()));
    }

    @Operation(summary = "加入购物车")
    @PostMapping
    public ApiResponse<Long> add(@Valid @RequestBody CartAddReq req) {
        return ApiResponse.success(orderService.addCart(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "更新购物车数量（按商品ID）")
    @PutMapping("/{productId}")
    public ApiResponse<Void> update(@PathVariable Long productId,
                                    @RequestBody Map<String, Object> body) {
        Long userId = UserContext.requiredUserId();
        // 找到该商品对应的购物车项
        CartItem item = orderService.listCart(userId).stream()
                .filter(c -> productId.equals(c.getProductId()))
                .findFirst()
                .orElse(null);
        if (item == null) {
            return ApiResponse.success();
        }
        CartUpdateReq req = new CartUpdateReq();
        req.setId(item.getId());
        if (body.get("quantity") != null) {
            req.setQuantity(Integer.valueOf(body.get("quantity").toString()));
        }
        if (body.get("isSelected") != null) {
            req.setIsSelected(Integer.valueOf(body.get("isSelected").toString()));
        }
        orderService.updateCart(req, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "删除购物车项（按商品ID）")
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(@PathVariable Long productId) {
        Long userId = UserContext.requiredUserId();
        CartItem item = orderService.listCart(userId).stream()
                .filter(c -> productId.equals(c.getProductId()))
                .findFirst()
                .orElse(null);
        if (item != null) {
            orderService.removeCart(item.getId(), userId);
        }
        return ApiResponse.success();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping("/clear")
    public ApiResponse<Void> clear() {
        orderService.clearSelectedCart(UserContext.requiredUserId());
        return ApiResponse.success();
    }
}
