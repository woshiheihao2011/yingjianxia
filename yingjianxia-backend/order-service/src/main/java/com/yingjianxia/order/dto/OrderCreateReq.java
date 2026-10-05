package com.yingjianxia.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 下单请求（买家端）
 */
@Data
@Schema(description = "下单请求")
public class OrderCreateReq {

    @Schema(description = "商品列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "下单商品列表不能为空")
    @Valid
    private List<OrderItemReq> items;

    @Schema(description = "收货地址ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "收货地址不能为空")
    private Long addressId;

    @Schema(description = "收货地址快照JSON（网关/前端传入，防修改）")
    private String addressSnapshot;

    @Schema(description = "优惠券ID")
    private Long couponId;

    @Schema(description = "使用积分抵扣")
    private Integer pointsUsed;

    @Schema(description = "买家留言")
    private String remark;

    @Schema(description = "支付方式：balance/wechat/alipay")
    private String paymentMethod;

    @Schema(description = "幂等键（防重复下单）")
    private String idempotencyKey;

    @Data
    @Schema(description = "下单商品项")
    public static class OrderItemReq {
        @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private Long productId;

        @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        private Integer quantity;

        @Schema(description = "卖家ID（多卖家场景下按店铺拆单用）")
        private Long sellerId;
    }
}
