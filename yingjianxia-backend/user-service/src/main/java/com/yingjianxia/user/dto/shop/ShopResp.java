package com.yingjianxia.user.dto.shop;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 店铺详情 响应 DTO
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "店铺详情响应")
public class ShopResp {

    @Schema(description = "店铺ID")
    private Long id;

    @Schema(description = "卖家用户ID")
    private Long sellerId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "店铺简介")
    private String description;

    @Schema(description = "店铺头像")
    private String logoUrl;

    @Schema(description = "封面图")
    private String coverUrl;

    @Schema(description = "Banner 图（与 coverUrl 同源，前端展示用）")
    private String bannerUrl;

    @Schema(description = "店铺评分 0.0-5.0")
    private BigDecimal rating;

    @Schema(description = "粉丝数")
    private Integer followerCount;

    @Schema(description = "成交量")
    private Integer saleCount;

    @Schema(description = "在售商品数")
    private Integer onSaleCount;

    @Schema(description = "评价数")
    private Integer reviewCount;

    @Schema(description = "销量（累计售出件数，与 saleCount 同义，前端展示用）")
    private Integer soldCount;

    @Schema(description = "是否认证优质卖家")
    private Boolean verified;

    @Schema(description = "卖家信用分")
    private BigDecimal sellerCreditScore;

    @Schema(description = "开店时间")
    private LocalDateTime createdAt;

    // ---------- 前端店铺主页展示扩展字段（bug-20260908130123）----------

    @Schema(description = "所在地区（取卖家 User.region）")
    private String region;

    @Schema(description = "入驻时间（yyyy-MM-dd 字符串，取 shop.createdAt）")
    private String openedAt;

    @Schema(description = "店铺等级名称：金牌店铺/普通店铺")
    private String shopLevel;

    @Schema(description = "月成交量")
    private Integer monthlySales;

    @Schema(description = "好评率（百分比，如 99.4）")
    private BigDecimal goodRate;

    @Schema(description = "响应速度（分钟）")
    private Integer responseTime;

    @Schema(description = "保证金（元）")
    private BigDecimal deposit;

    @Schema(description = "评分细分：描述相符 0.0-5.0")
    private BigDecimal descScore;

    @Schema(description = "评分细分：卖家服务 0.0-5.0")
    private BigDecimal serviceScore;

    @Schema(description = "评分细分：物流发货 0.0-5.0")
    private BigDecimal shipScore;

    @Schema(description = "认证标签列表")
    private List<String> certifications;

    // ---------- 店铺设置冗余返回 ----------

    @Schema(description = "店铺设置：客服电话")
    private String contactPhone;

    @Schema(description = "店铺设置：客服微信")
    private String contactWechat;

    @Schema(description = "店铺设置：营业时间")
    private String businessHours;

    @Schema(description = "店铺设置：默认快递")
    private String defaultExpress;

    @Schema(description = "店铺设置：是否接受砍价")
    private Boolean acceptBargain;

    @Schema(description = "店铺设置：是否支持面交")
    private Boolean supportFaceTrade;

    @Schema(description = "店铺设置：发货时效承诺(小时)")
    private Integer shipTimePromise;

    @Schema(description = "店铺设置：店铺公告")
    private String announcement;
}
