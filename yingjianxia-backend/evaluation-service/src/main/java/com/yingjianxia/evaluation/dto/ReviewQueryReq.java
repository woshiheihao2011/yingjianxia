package com.yingjianxia.evaluation.dto;

import com.yingjianxia.common.core.result.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评价查询请求（按商品/卖家查询，支持评分筛选）
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "评价查询")
public class ReviewQueryReq extends PageQuery {

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "卖家ID")
    private Long sellerId;

    @Schema(description = "评分筛选（1~5）")
    private Integer rating;
}
