package com.yingjianxia.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品实体 — products 表
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("products")
public class Product extends BaseEntity {

    /** 卖家用户ID */
    private Long sellerId;

    /** 所属店铺ID */
    private Long shopId;

    /** 商品分类 ID */
    private Integer categoryId;

    /** 商品标题（2-30字） */
    private String title;

    /** 售价 */
    private BigDecimal price;

    /** 原价（划线价） */
    private BigDecimal originalPrice;

    /** 成色：1全新 2-99新 3-95成新 4-9成新 5-战损版 */
    private Integer conditionLevel;

    /** 品牌 */
    private String brand;

    /** 副标题（简短卖点，可选） */
    private String subTitle;

    /** 购买渠道 */
    private String purchaseChannel;

    /** 箱说全 */
    private Boolean hasBox;

    /** 商品描述（富文本/Markdown） */
    private String description;

    /** 所在地 */
    private String location;

    /** 库存数量 */
    private Integer stock;

    /** 库存预警阈值 */
    private Integer warningThreshold;

    /** 0草稿 1审核中 2在售 3已售 4下架 5审核拒绝 */
    private Integer status;

    /** 审核拒绝原因 */
    private String rejectReason;

    /** 浏览量（Redis 异步回写） */
    private Integer viewCount;

    /** 收藏数 */
    private Integer favoriteCount;

    /** 想要数（独立于收藏） */
    private Integer wantCount;

    /** 销量（已售数量） */
    private Integer sales;

    /** 商品编码/SKU */
    private String sku;

    /** 瑕疵列表（JSON 数组，格式：[{"area":"外观","detail":"右下角轻微磕碰"}]） */
    private String defectsJson;

    /** 是否包邮 */
    private Boolean shipFree;

    /** 运费模板（描述性字符串，如"全国包邮（顺丰）"） */
    private String shipTemplate;

    /** 质保说明（如"1年店保"） */
    private String warranty;

    /** 售后类型（如 7-days / 15-days / none） */
    private String aftersalesType;

    /** 服务承诺标签（JSON 数组，格式：["支持担保交易","7天无理由"]） */
    private String serviceTagsJson;

    /** 发布时间 */
    private LocalDateTime publishedAt;

    /* ========== deleted/version 由 BaseEntity 继承，表已有对应列 ========== */

    /* ========== 非持久化便捷字段 ========== */
    /** 图片列表 */
    @TableField(exist = false)
    private List<ProductImage> images;

    /** 规格参数列表 */
    @TableField(exist = false)
    private List<ProductSpec> specs;

    /* ========== 状态常量（商品状态机） ========== */
    /** 草稿 */
    public static final int STATUS_DRAFT = 0;
    /** 审核中 */
    public static final int STATUS_REVIEWING = 1;
    /** 在售 */
    public static final int STATUS_ON_SALE = 2;
    /** 已售罄（保留） */
    public static final int STATUS_SOLD_OUT = 3;
    /** 卖家下架 */
    public static final int STATUS_OFF_SHELF = 4;
    /** 审核拒绝 */
    public static final int STATUS_REJECTED = 5;

    /* ========== 成色常量（5 档） ========== */
    /** 全新 */
    public static final int COND_NEW = 1;
    /** 99新/准新 */
    public static final int COND_99 = 2;
    /** 95成新 */
    public static final int COND_95 = 3;
    /** 9成新 */
    public static final int COND_90 = 4;
    /** 战损版/8成新及以下 */
    public static final int COND_WORN = 5;
}
