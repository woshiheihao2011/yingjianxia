package com.yingjianxia.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 卖家创建营销活动请求
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "卖家创建营销活动请求")
public class PromotionCreateReq {

    @Schema(description = "所属店铺ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "店铺ID不能为空")
    private Long shopId;

    @Schema(description = "类型：1限时折扣 2满减 3包邮", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "活动类型不能为空")
    @Min(value = 1, message = "活动类型不合法") @Max(value = 3, message = "活动类型不合法")
    private Integer type;

    @Schema(description = "活动名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "活动名称不能为空")
    private String name;

    @Schema(description = "折扣率（type=1）")
    private BigDecimal discountRate;

    @Schema(description = "满减门槛（type=2）")
    private BigDecimal minSpend;

    @Schema(description = "满减金额（type=2）")
    private BigDecimal reduceAmount;

    @Schema(description = "参与活动商品ID列表（JSON）")
    private String productIds;

    @Schema(description = "活动库存（限量）")
    private Integer activityStock;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startAt;

    @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endAt;
}
