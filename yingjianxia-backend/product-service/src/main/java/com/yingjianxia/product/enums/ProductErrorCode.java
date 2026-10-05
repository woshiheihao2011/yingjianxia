package com.yingjianxia.product.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品域错误码枚举（2000 ~ 2999）
 *
 * @author 硬件侠后端团队
 */
@Getter
@AllArgsConstructor
public enum ProductErrorCode implements IErrorCode {

    /* ---------- 商品基础 2000 ~ 2099 ---------- */
    PARAM_INVALID(2000, "参数错误"),
    CATEGORY_NOT_FOUND(2001, "商品分类不存在"),
    CATEGORY_DISABLED(2002, "该分类已禁用"),
    TITLE_TOO_SHORT(2003, "商品标题至少2个字符"),
    TITLE_TOO_LONG(2004, "商品标题最多30个字符"),
    PRICE_INVALID(2005, "商品价格必须大于0且不超过100万"),
    CONDITION_INVALID(2006, "成色参数无效（1全新/2-99新/3-9成新/4-战损）"),
    DESCRIPTION_TOO_LONG(2007, "商品描述超过10000字符"),
    IMAGE_REQUIRED(2008, "至少上传1张商品图片"),
    IMAGE_LIMIT_EXCEEDED(2009, "商品图片最多20张"),
    PRODUCT_NOT_FOUND(2010, "商品不存在或已删除"),
    PRODUCT_NOT_ON_SALE(2011, "商品已下架或不在售"),
    PRODUCT_NOT_BELONG_SELLER(2012, "无权操作该商品"),
    STOCK_NOT_ENOUGH(2013, "商品库存不足"),
    STOCK_NEGATIVE(2014, "库存不能为负数"),

    /* ---------- 状态机流转 2100 ~ 2199 ---------- */
    STATUS_ILLEGAL_TRANSITION(2101, "非法的商品状态流转"),
    STATUS_MUST_BE_DRAFT_OR_REJECTED(2102, "仅草稿或审核拒绝的商品可提交审核"),
    STATUS_MUST_BE_REVIEWING(2103, "仅审核中的商品可执行审核操作"),
    STATUS_MUST_BE_ON_SALE(2104, "仅在售状态可执行此操作"),
    INSPECTION_REQUIRED_FOR_PUBLISH(2105, "平台规则：显卡/CPU必须完成验机才能发布"),

    /* ---------- 收藏 2200 ~ 2299 ---------- */
    FAVORITE_ALREADY(2201, "该商品已在收藏夹中"),
    FAVORITE_NOT_EXISTS(2202, "未收藏该商品"),

    /* ---------- 浏览量/图片/快照 2300 ~ 2399 ---------- */
    IMAGE_UPLOAD_FAIL(2301, "图片上传失败"),
    IMAGE_TYPE_INVALID(2302, "仅支持 JPG/PNG/WebP 格式的图片"),
    SNAPSHOT_NOT_FOUND(2303, "商品快照不存在"),
    CANAL_SYNC_DELAY(2304, "ES 同步延迟，请稍后再试"),

    /* ---------- 库存 2400 ~ 2499 ---------- */
    STOCK_LOCK_FAIL(2401, "库存预扣失败（可能已被抢光）"),
    STOCK_UNLOCK_KEY_INVALID(2402, "库存解锁 Key 无效或已过期");

    private final Integer code;
    private final String message;
}
