package com.yingjianxia.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情 响应 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "商品详情响应")
public class ProductDetailResp {

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "卖家用户ID")
    private Long sellerId;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "店铺名称（冗余）")
    private String shopName;

    @Schema(description = "店铺头像（冗余）")
    private String shopLogoUrl;

    @Schema(description = "店铺是否优质卖家（冗余）")
    private Boolean shopVerified;

    @Schema(description = "卖家信用分（冗余）")
    private BigDecimal sellerCreditScore;

    @Schema(description = "分类 ID")
    private Integer categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "副标题（简短卖点）")
    private String subTitle;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "成色 1全新/2-99新/3-95成新/4-9成新/5-战损版")
    private Integer conditionLevel;

    @Schema(description = "成色显示名", example = "99新")
    private String conditionName;

    @Schema(description = "购买渠道")
    private String purchaseChannel;

    @Schema(description = "箱说全")
    private Boolean hasBox;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "所在地")
    private String location;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "审核拒绝原因（仅卖家本人可见）")
    private String rejectReason;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "收藏数")
    private Integer favoriteCount;

    @Schema(description = "想要数")
    private Integer wantCount;

    @Schema(description = "销量")
    private Integer sales;

    @Schema(description = "商品编码/SKU")
    private String sku;

    @Schema(description = "当前用户是否已收藏")
    private Boolean favoredByMe;

    @Schema(description = "库存预警阈值")
    private Integer warningThreshold;

    @Schema(description = "发布时间")
    private LocalDateTime publishedAt;

    @Schema(description = "是否关联了验机报告")
    private Boolean hasInspection;

    @Schema(description = "验机报告 ID（未验机 null）")
    private Long inspectionId;

    @Schema(description = "图片 URL（按顺序，第一张主图）")
    private List<String> imageUrls;

    @Schema(description = "规格参数")
    private List<ProductSaveReq.SpecItem> specs;

    @Schema(description = "瑕疵列表")
    private List<ProductSaveReq.DefectItem> defects;

    @Schema(description = "是否包邮")
    private Boolean shipFree;

    @Schema(description = "运费模板（描述性）")
    private String shipTemplate;

    @Schema(description = "质保说明")
    private String warranty;

    @Schema(description = "售后类型")
    private String aftersalesType;

    @Schema(description = "服务承诺标签")
    private List<String> serviceTags;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
