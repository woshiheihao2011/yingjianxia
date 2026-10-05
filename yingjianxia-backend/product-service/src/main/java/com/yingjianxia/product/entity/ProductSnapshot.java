package com.yingjianxia.product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yingjianxia.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品快照实体 — product_snapshots 表
 * <p>
 * 用途：用户下单时生成快照，保存商品发布时的标题/价格/成色/图片/规格/描述，
 * 防止后续卖家修改导致订单争议时无据可依。验机报告关联快照，不做物理修改。
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_snapshots")
public class ProductSnapshot extends BaseEntity {

    /** 关联商品（原商品ID，删除后仍保留） */
    private Long productId;

    /** 关联订单号（哪个订单生成的快照） */
    private Long orderId;

    /** 卖家ID（冗余） */
    private Long sellerId;

    /** 分类 ID */
    private Integer categoryId;

    /** 标题 */
    private String title;

    /** 售价 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 成色 */
    private Integer conditionLevel;

    /** 购买渠道 */
    private String purchaseChannel;

    /** 箱说全 */
    private Boolean hasBox;

    /** 描述（原始值） */
    private String description;

    /** 所在地 */
    private String location;

    /** 图片 JSON 数组（发布时完整图片 URL） */
    private String imagesJson;

    /** 规格参数 JSON 数组 */
    private String specsJson;

    /** 验机报告ID（若卖家选了验机服务，发布后生成，快照绑定） */
    private Long inspectionId;

    /** 验机报告签名摘要（SHA-256）— 防篡改校验冗余 */
    private String reportSignature;

    /* ========== deleted/version/updatedAt 由 BaseEntity 继承，表已有对应列 ========== */
}
