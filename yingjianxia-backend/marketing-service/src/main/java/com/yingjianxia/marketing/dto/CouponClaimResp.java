package com.yingjianxia.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券领取响应
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "优惠券领取响应")
public class CouponClaimResp {

    @Schema(description = "领券记录ID")
    private Long recordId;

    @Schema(description = "优惠券ID")
    private Long couponId;

    @Schema(description = "券名称")
    private String couponName;

    @Schema(description = "券类型：1满减 2折扣 3包邮")
    private Integer couponType;

    @Schema(description = "面额")
    private BigDecimal faceValue;

    @Schema(description = "折扣率")
    private BigDecimal discountRate;

    @Schema(description = "使用门槛")
    private BigDecimal minSpend;

    @Schema(description = "过期时间")
    private LocalDateTime expireAt;
}
