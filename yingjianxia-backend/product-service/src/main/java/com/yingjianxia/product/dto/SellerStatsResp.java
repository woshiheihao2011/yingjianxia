package com.yingjianxia.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 卖家商品统计 响应 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "卖家商品统计响应")
public class SellerStatsResp {

    @Schema(description = "商品总数（不含已删除）")
    private Long totalProducts;

    @Schema(description = "草稿数")
    private Long draftCount;

    @Schema(description = "审核中数")
    private Long reviewingCount;

    @Schema(description = "在售数")
    private Long onSaleCount;

    @Schema(description = "已售罄数")
    private Long soldOutCount;

    @Schema(description = "审核拒绝数")
    private Long rejectedCount;

    @Schema(description = "已下架数")
    private Long offShelfCount;

    @Schema(description = "累计销量")
    private Long totalSales;

    @Schema(description = "累计浏览量")
    private Long totalViews;

    @Schema(description = "累计收藏数")
    private Long totalFavorites;
}
