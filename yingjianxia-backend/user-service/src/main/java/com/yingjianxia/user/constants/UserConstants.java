package com.yingjianxia.user.constants;

/**
 * 用户域业务常量
 *
 * @author 硬件侠后端团队
 */
public interface UserConstants {

    /* ========== 短信验证码场景 ========== */
    /** 注册 */
    String SMS_SCENE_REGISTER = "register";
    /** 登录 */
    String SMS_SCENE_LOGIN = "login";
    /** 找回密码 */
    String SMS_SCENE_RESET_PWD = "resetPwd";
    /** 实名认证 */
    String SMS_SCENE_REAL_NAME = "realName";
    /** 修改绑定手机 */
    String SMS_SCENE_CHANGE_PHONE = "changePhone";

    /** 验证码 TTL：5 分钟 */
    long SMS_CODE_TTL_SEC = 5 * 60;
    /** 同手机号发送频率限制：60 秒 */
    long SMS_FREQ_LIMIT_SEC = 60;

    /* ========== 账号安全 ========== */
    /** 连续密码错误次数阈值，超过锁定账号 */
    int LOGIN_FAIL_THRESHOLD = 5;
    /** 错误计数 / 锁定 TTL：30 分钟 */
    long LOGIN_FAIL_TTL_SEC = 30 * 60;
    /** 密码强度正则：8-20位，至少含大小写字母和数字 */
    String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&_\\-#.]{8,20}$";

    /* ========== 用户角色 ========== */
    String ROLE_BUYER = "BUYER";
    String ROLE_SELLER = "SELLER";
    String ROLE_ADMIN = "ADMIN";
    String ROLE_AUDITOR = "AUDITOR";
    String ROLE_INSPECTOR = "INSPECTOR";
    String ROLE_CS = "CS";

    /* ========== 默认配置 ========== */
    /** 新用户默认信用分 */
    java.math.BigDecimal DEFAULT_CREDIT_SCORE = new java.math.BigDecimal("5.0");
    /** 收货地址最大数量 */
    int MAX_ADDRESS_COUNT = 20;
    /** 优质卖家认证：成交笔数阈值 */
    int SELLER_VERIFY_TX_THRESHOLD = 10;
    /** 优质卖家认证：信用分阈值 */
    java.math.BigDecimal SELLER_VERIFY_CREDIT_THRESHOLD = new java.math.BigDecimal("4.5");
    /** 默认店铺评分 */
    java.math.BigDecimal DEFAULT_SHOP_RATING = new java.math.BigDecimal("5.0");
}
