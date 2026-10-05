package com.yingjianxia.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量改价 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "批量改价请求")
public class BatchPriceUpdateReq {

    @Schema(description = "待改价商品列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "改价列表不能为空")
    @Valid
    private List<Item> items;

    @Data
    @Schema(name = "批量改价项")
    public static class Item {
        @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long productId;

        @Schema(description = "新价格", requiredMode = Schema.RequiredMode.REQUIRED)
        private BigDecimal price;
    }
}
