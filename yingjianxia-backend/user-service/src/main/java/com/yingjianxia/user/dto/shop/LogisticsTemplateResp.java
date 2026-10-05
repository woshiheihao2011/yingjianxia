package com.yingjianxia.user.dto.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运费模板 响应 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "运费模板响应")
public class LogisticsTemplateResp {

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "计费方式：weight/volume/fixed")
    private String type;

    @Schema(description = "首件/首重运费")
    private BigDecimal baseFee;

    @Schema(description = "首件/首重数量")
    private Integer baseUnit;

    @Schema(description = "续件/续重运费")
    private BigDecimal stepFee;

    @Schema(description = "续件/续重数量")
    private Integer stepUnit;

    @Schema(description = "包邮门槛金额")
    private BigDecimal freeShippingThreshold;

    @Schema(description = "是否默认模板")
    private Boolean isDefault;

    @Schema(description = "适用地区（JSON）")
    private String region;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
