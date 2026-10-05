package com.yingjianxia.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 下单时使用优惠券请求
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "下单时使用优惠券请求")
public class CouponUseReq {

    @Schema(description = "领券记录ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "领券记录ID不能为空")
    private Long couponRecordId;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "订单金额（用于校验门槛）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单金额不能为空")
    private BigDecimal orderAmount;

    @Schema(description = "商品ID列表（用于校验适用范围）")
    @NotEmpty(message = "商品ID列表不能为空")
    private java.util.List<Long> productIds;
}
