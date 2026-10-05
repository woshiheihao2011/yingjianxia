package com.yingjianxia.product.dto;

import com.yingjianxia.common.core.result.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品搜索/列表查询 请求 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商品列表查询请求")
public class ProductQueryReq extends PageQuery {

    @Schema(description = "关键字（标题/品牌等，走ES优先；降级走 LIKE）")
    private String keyword;

    @Schema(description = "分类ID列表（多选）", example = "[1,2]")
    private List<Integer> categoryIds;

    @Schema(description = "成色过滤：1全新/2-99新/3-9成新/4-战损版")
    private List<Integer> conditionLevels;

    @Schema(description = "最低售价")
    private BigDecimal priceMin;

    @Schema(description = "最高售价")
    private BigDecimal priceMax;

    @Schema(description = "所在地（省市）")
    private String location;

    @Schema(description = "是否仅看已验机")
    private Boolean onlyInspected;

    @Schema(description = "是否仅看优质卖家")
    private Boolean onlyVerifiedSeller;

    @Schema(description = "箱说全")
    private Boolean onlyWithBox;

    @Schema(description = "排序字段：price/viewCount/publishedAt/rating", defaultValue = "publishedAt")
    private String sortField = "publishedAt";

    @Schema(description = "排序方向：asc/desc", defaultValue = "desc")
    private String sortDirection = "desc";

    @Schema(description = "[卖家端] 按卖家ID查自己的商品（我的发布）")
    private Long sellerId;

    @Schema(description = "[卖家端] 状态过滤（仅卖家自己看时生效）")
    private Integer status;
}
