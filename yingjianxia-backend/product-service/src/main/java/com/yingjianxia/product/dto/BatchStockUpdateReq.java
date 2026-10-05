package com.yingjianxia.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量改库存 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "批量改库存请求")
public class BatchStockUpdateReq {

    @Schema(description = "待改库存商品列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "改库存列表不能为空")
    @Valid
    private List<Item> items;

    @Data
    @Schema(name = "批量改库存项")
    public static class Item {
        @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long productId;

        @Schema(description = "新库存数量", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer stock;

        @Schema(description = "库存预警阈值（可选）")
        private Integer warningThreshold;
    }
}
