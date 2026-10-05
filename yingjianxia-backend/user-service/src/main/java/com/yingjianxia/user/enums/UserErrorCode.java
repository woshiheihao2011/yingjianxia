package com.yingjianxia.user.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户域错误码枚举（1000 ~ 1999）
 *
 * @author 硬件侠后端团队
 */
@Getter
@AllArgsConstructor
public enum UserErrorCode implements IErrorCode {

    /* ---------- 认证类 1000 ~ 1099 ---------- */
    SMS_CODE_SEND_FREQUENT(1001, "验证码发送过于频繁，请60秒后再试"),
    SMS_CODE_INVALID(1002, "验证码错误或已过期"),
    SMS_SCENE_NOT_SUPPORTED(1003, "不支持的验证码场景"),
    PASSWORD_NOT_MATCH(1004, "两次输入的密码不一致"),
    PASSWORD_STRENGTH_WEAK(1005, "密码强度不足，需包含大小写字母和数字，长度8-20位"),
    PHONE_FORMAT_ERROR(1006, "手机号格式错误"),
    OAUTH_PROVIDER_NOT_SUPPORTED(1007, "不支持的第三方登录方式"),
    OAUTH_CODE_INVALID(1008, "第三方授权码无效"),
    TOKEN_REFRESH_REQUIRED(1009, "请使用 Refresh Token 调用刷新接口"),
    REFRESH_TOKEN_INVALID(1010, "Refresh Token 无效或已过期"),
    REAL_NAME_DUPLICATE(1011, "该用户已提交实名认证，请勿重复申请"),
    REAL_NAME_ID_CARD_USED(1012, "该身份证号已被其他用户认证"),
    REAL_NAME_VERIFY_FAILED(1013, "实名信息与公安系统不匹配"),
    OLD_PASSWORD_ERROR(1014, "原密码错误"),

    /* ---------- 用户资料类 1100 ~ 1199 ---------- */
    NICKNAME_DUPLICATE(1101, "该昵称已被使用"),
    NICKNAME_TOO_LONG(1102, "昵称长度不能超过50个字符"),
    BIO_TOO_LONG(1103, "个人简介长度不能超过200个字符"),
    AVATAR_UPLOAD_FAIL(1104, "头像上传失败"),
    USER_NOT_FOUND(1105, "用户不存在"),
    EMAIL_DUPLICATE(1106, "该邮箱已被使用"),
    DEVICE_NOT_FOUND(1107, "设备不存在或已退出"),
    DEVICE_KICK_CURRENT_FORBIDDEN(1108, "不能踢出当前设备，请使用登出接口"),

    /* ---------- 地址类 1200 ~ 1299 ---------- */
    ADDRESS_LIMIT_EXCEEDED(1201, "收货地址数量已达上限（最多20个）"),
    ADDRESS_NOT_FOUND(1202, "收货地址不存在"),
    ADDRESS_NOT_BELONG_TO_USER(1203, "该地址不属于当前用户"),
    DEFAULT_ADDRESS_CANNOT_DELETE(1204, "默认地址不能删除，请先将其他地址设为默认"),

    /* ---------- 店铺类 1300 ~ 1399 ---------- */
    SHOP_ALREADY_EXISTS(1301, "您已开通店铺，每人限开一家"),
    SHOP_NOT_FOUND(1302, "店铺不存在"),
    SHOP_NOT_BELONG_TO_USER(1303, "无权操作该店铺"),
    SHOP_NAME_TOO_LONG(1304, "店铺名称不能超过100个字符"),
    SHOP_NAME_DUPLICATE(1305, "该店铺名称已被使用"),
    SELLER_NOT_VERIFIED(1306, "需要通过优质卖家认证才能使用该功能"),
    SELLER_VERIFY_ALREADY(1307, "您已通过优质卖家认证"),
    SELLER_VERIFY_INSUFFICIENT_TX(1308, "优质卖家认证需累计至少10笔成交"),
    SELLER_VERIFY_LOW_CREDIT(1309, "优质卖家认证需信用分≥4.5"),
    SELLER_VERIFY_NO_REALNAME(1310, "请先完成实名认证后再申请优质卖家"),
    SELLER_VERIFY_NO_SHOP(1311, "请先开通店铺后再申请优质卖家认证"),

    /* ---------- 评价类 1400 ~ 1499 ---------- */
    REVIEW_ALREADY_EXISTS(1401, "该订单已评价，请勿重复提交"),
    REVIEW_RATING_INVALID(1402, "评分必须在1-5星之间"),
    REVIEW_NOT_FOUND(1403, "评价不存在"),
    REVIEW_NOT_BELONG_TO_USER(1404, "无权操作该评价"),
    REVIEW_APPEND_TIMEOUT(1405, "追评需在收货后180天内完成"),
    REVIEW_CONTENT_TOO_LONG(1406, "评价内容不能超过1000个字符"),
    REVIEW_REPLY_DUPLICATE(1407, "卖家只能回复一次评价");

    /* ---------- 字段 ---------- */
    private final Integer code;
    private final String message;
}
