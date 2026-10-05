package com.yingjianxia.common.core.constants;

/**
 * 全局 Redis Key 常量
 * <p>
 * 命名约定：
 * <pre>
 *   yjx:{业务域}:{资源标识}:[动态参数]
 * </pre>
 * 示例：{@code yjx:user:sms:13812345678}
 *
 * @author 硬件侠后端团队
 */
public interface RedisKeyConstants {

    /* ---------- 前缀 ---------- */
    String PREFIX = "yjx";

    /* ============================================================
     *  1000 ~ 1999 : 用户域
     * ============================================================ */

    /** 短信验证码：yjx:user:sms:{phone}:{scene} | TTL: 5min */
    String SMS_CODE = PREFIX + ":user:sms:%s:%s";
    /** 短信发送频率限制：yjx:user:sms:freq:{phone} | TTL: 60s */
    String SMS_FREQ_LIMIT = PREFIX + ":user:sms:freq:%s";
    /** JWT Token 黑名单：yjx:user:token:blacklist:{jti} | TTL: 2h */
    String TOKEN_BLACKLIST = PREFIX + ":user:token:blacklist:%s";
    /** 密码错误计数：yjx:user:login:fail:{userId或phone} | TTL: 30min */
    String LOGIN_FAIL_COUNT = PREFIX + ":user:login:fail:%s";
    /** 账号锁定标记：yjx:user:lock:{userId} | TTL: 30min */
    String ACCOUNT_LOCK = PREFIX + ":user:lock:%s";
    /** 已绑定第三方账号：yjx:user:oauth:{type}:{openId} → userId */
    String OAUTH_BIND = PREFIX + ":user:oauth:%s:%s";
    /** 用户活跃登录设备(Hash)：yjx:user:token:active:{userId} field=jti value=设备信息JSON | TTL: access token有效期 */
    String USER_TOKEN_ACTIVE = PREFIX + ":user:token:active:%s";

    /* ============================================================
     *  2000 ~ 2999 : 商品域
     * ============================================================ */

    /** 商品浏览量 INCR：yjx:product:view:{productId} */
    String PRODUCT_VIEW_COUNT = PREFIX + ":product:view:%s";
    /** 商品浏览量批量同步 dirty标记 SET：yjx:product:view:dirty_ids */
    String PRODUCT_VIEW_DIRTY_SET = PREFIX + ":product:view:dirty_ids";
    /** 商品库存预扣（购物车/下单）：yjx:product:stock:reserved:{productId} */
    String PRODUCT_STOCK_RESERVED = PREFIX + ":product:stock:reserved:%s";
    /** ES 热门搜索词 ZSet：yjx:product:search:hot_words */
    String SEARCH_HOT_WORDS = PREFIX + ":product:search:hot_words";

    /* ============================================================
     *  4000 ~ 4999 : 订单交易域
     * ============================================================ */

    /** 购物车：yjx:order:cart:{userId} → Hash field=productId, value=spec:qty */
    String CART_KEY = PREFIX + ":order:cart:%s";
    /** 订单防重（幂等键）：yjx:order:idempotent:{key} | TTL: 30min */
    String ORDER_IDEMPOTENT = PREFIX + ":order:idempotent:%s";
    /** 超时取消任务分布式锁 SETNX：yjx:order:lock:cancel_timeout */
    String ORDER_CANCEL_LOCK = PREFIX + ":order:lock:cancel_timeout";

    /* ============================================================
     *  5000 ~ 5999 : 担保资金域
     * ============================================================ */

    /** 钱包更新分布式锁：yjx:escrow:wallet:lock:{userId} */
    String WALLET_LOCK = PREFIX + ":escrow:wallet:lock:%s";
    /** TCC 空回滚标记：yjx:escrow:tcc:cancel_mark:{idempotencyKey} | TTL: 24h */
    String TCC_CANCEL_MARK = PREFIX + ":escrow:tcc:cancel_mark:%s";
    /** TCC 悬挂防护（Try先到标记）：yjx:escrow:tcc:try_mark:{idempotencyKey} */
    String TCC_TRY_MARK = PREFIX + ":escrow:tcc:try_mark:%s";

    /* ============================================================
     *  营销域
     * ============================================================ */
    /** 优惠券剩余库存预扣：yjx:mkt:coupon:stock:{couponId} */
    String COUPON_STOCK = PREFIX + ":mkt:coupon:stock:%s";

    /* ============================================================
     *  消息域
     * ============================================================ */
    /** 用户未读数：yjx:msg:unread:{userId} */
    String MSG_UNREAD = PREFIX + ":msg:unread:%s";
    /** WebSocket 在线节点：yjx:ws:online:{userId} → nodeId */
    String WS_USER_NODE = PREFIX + ":ws:online:%s";

    /* ============================================================
     *  通用幂等键（供 common-web 拦截器）
     * ============================================================ */
    /** API 通用幂等：yjx:api:idempotent:{key} | TTL: 默认 60s */
    String API_IDEMPOTENT = PREFIX + ":api:idempotent:%s";

    /* ============================================================
     *  分布式锁前缀
     * ============================================================ */
    String LOCK_PREFIX = PREFIX + ":lock:%s";
}
