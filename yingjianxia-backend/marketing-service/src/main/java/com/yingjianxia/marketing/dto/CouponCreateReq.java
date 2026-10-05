package com.yingjianxia.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 卖家创建优惠券请求
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "卖家创建优惠券请求")
public class CouponCreateReq {

    @Schema(description = "所属店铺ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    @Schema(description = "券名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "券名称不能为空")
    private String name;

    @Schema(description = "类型：1满减券 2折扣券 3包邮券", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "券类型不能为空")
    @Min(value = 1, message = "券类型不合法") @Max(value = 3, message = "券类型不合法")
    private Integer couponType;

    @Schema(description = "面额（满减券使用）")
    private BigDecimal faceValue;

    @Schema(description = "折扣率（折扣券使用，0.85=8.5折）")
    private BigDecimal discountRate;

    @Schema(description = "使用门槛（满X可用，默认0）")
    private BigDecimal minSpend;

    @Schema(description = "发放总量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "发放总量不能为空")
    @Min(value = 1, message = "发放总量必须大于0")
    private Integer totalCount;

    @Schema(description = "每人限领，默认1")
    @Min(value = 1, message = "每人限领必须大于0")
    private Integer perUserLimit;

    @Schema(description = "适用范围：0全店 1指定品类 2指定商品", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "适用范围不能为空")
    @Min(value = 0, message = "适用范围不合法") @Max(value = 2, message = "适用范围不合法")
    private Integer scope;

    @Schema(description = "适用范围值（品类ID/商品ID JSON）")
    private String scopeValue;

    @Schema(description = "有效期开始", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "有效期开始不能为空")
    private LocalDateTime validStart;

    @Schema(description = "有效期结束", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "有效期结束不能为空")
    private LocalDateTime validEnd;
}
