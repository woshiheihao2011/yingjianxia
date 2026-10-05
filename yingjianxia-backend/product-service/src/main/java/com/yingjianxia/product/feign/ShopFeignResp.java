package com.yingjianxia.product.feign;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 店铺 Feign 响应 DTO（product-service 调 user-service 时使用）
 * 只包含 product-service 需要冗余到商品详情的字段
 *
 * @author 硬件侠后端团队
 */
@Data
public class ShopFeignResp {

    /** 店铺ID */
    private Long id;

    /** 卖家用户ID */
    private Long sellerId;

    /** 店铺名称 */
    private String shopName;

    /** 店铺头像 */
    private String logoUrl;

    /** 是否优质卖家 */
    private Boolean verified;

    /** 店铺评分 */
    private BigDecimal rating;
}
