package com.yingjianxia.user.dto.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 运费模板 创建/更新 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "运费模板请求")
public class LogisticsTemplateReq {

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称过长")
    private String name;

    @Schema(description = "计费方式：weight/volume/fixed", example = "weight")
    private String type = "weight";

    @Schema(description = "首件/首重运费")
    private BigDecimal baseFee = BigDecimal.ZERO;

    @Schema(description = "首件/首重数量")
    private Integer baseUnit = 1;

    @Schema(description = "续件/续重运费")
    private BigDecimal stepFee = BigDecimal.ZERO;

    @Schema(description = "续件/续重数量")
    private Integer stepUnit = 1;

    @Schema(description = "包邮门槛金额（null 表示不包邮）")
    private BigDecimal freeShippingThreshold;

    @Schema(description = "是否默认模板")
    private Boolean isDefault = false;

    @Schema(description = "适用地区（JSON 数组字符串）")
    private String region;
}
