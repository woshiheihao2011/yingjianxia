package com.yingjianxia.common.core.constants;

/**
 * RocketMQ Topic / Tag 常量
 * <p>
 * 命名约定：
 * <pre>
 *   Topic   : {业务域}_{动作}_TOPIC  例：ORDER_CREATED_TOPIC
 *   Tag     : {事件语义}              例：OrderCreated
 *   Group   : GID_{服务}_{Topic}_{动作}_CONSUMER
 * </pre>
 *
 * @author 硬件侠后端团队
 */
public interface MQConstants {

    /* ============================================================
     *  用户域
     * ============================================================ */
    String TOPIC_USER_EVENTS = "USER_EVENTS_TOPIC";
    String TAG_USER_REGISTERED    = "UserRegistered";
    String TAG_USER_REALNAME_OK   = "UserRealNameApproved";
    String TAG_CREDIT_SCORE_CHANGED = "CreditScoreChanged";
    String TAG_SHOP_CREATED       = "ShopCreated";

    String GROUP_USER_EVENTS_CONSUMER  = "GID_USER_EVENTS_COMMON_CONSUMER";
    String GROUP_SHOP_RATING_CONSUMER  = "GID_SHOP_RATING_UPDATE_CONSUMER";

    /* ============================================================
     *  商品域
     * ============================================================ */
    String TOPIC_PRODUCT_EVENTS = "PRODUCT_EVENTS_TOPIC";
    String TAG_PRODUCT_CREATED     = "ProductCreated";
    String TAG_PRODUCT_AUDIT_PASS  = "ProductAuditPassed";
    String TAG_PRODUCT_AUDIT_REJECT = "ProductAuditRejected";
    String TAG_PRODUCT_OFF_SHELF   = "ProductOffShelf";
    String TAG_PRODUCT_SOLD        = "ProductSold";

    String GROUP_PRODUCT_AUDIT_CONSUMER = "GID_PRODUCT_AUDIT_HANDLE_CONSUMER";
    String GROUP_PRODUCT_ES_SYNC_CONSUMER = "GID_PRODUCT_ES_SYNC_CONSUMER";

    /* ============================================================
     *  验机域
     * ============================================================ */
    String TOPIC_INSPECTION_EVENTS = "INSPECTION_EVENTS_TOPIC";
    String TAG_INSPECTION_STARTED   = "InspectionStarted";
    String TAG_INSPECTION_COMPLETED = "InspectionCompleted";

    String GROUP_INSPECTION_COMPLETED_CONSUMER = "GID_INSPECTION_COMPLETED_PUSH_CONSUMER";

    /* ============================================================
     *  订单域
     * ============================================================ */
    String TOPIC_ORDER_EVENTS = "ORDER_EVENTS_TOPIC";
    String TAG_ORDER_CREATED    = "OrderCreated";
    String TAG_ORDER_PAID       = "OrderPaid";       // 支付成功（担保资金已冻结）
    String TAG_ORDER_SHIPPED    = "OrderShipped";
    String TAG_ORDER_CONFIRMED  = "OrderConfirmed"; // 确认收货 → 放款
    String TAG_ORDER_CANCELLED  = "OrderCancelled";
    String TAG_ORDER_COMPLETED  = "OrderCompleted";  // 最终完成

    String GROUP_ORDER_EVENTS_CONSUMER    = "GID_ORDER_EVENTS_ORCH_CONSUMER";
    String GROUP_ORDER_NOTIFY_CONSUMER    = "GID_ORDER_MSG_PUSH_CONSUMER";
    String GROUP_ORDER_SHOP_STATS_CONSUMER = "GID_ORDER_SHOP_STATS_CONSUMER";

    /* ============================================================
     *  支付域
     * ============================================================ */
    String TOPIC_PAYMENT_EVENTS = "PAYMENT_EVENTS_TOPIC";
    String TAG_PAYMENT_SUCCESS = "PaymentSuccess";
    String TAG_PAYMENT_REFUND  = "PaymentRefund";
    String TAG_PAYMENT_FAILED  = "PaymentFailed";
    String TAG_WALLET_RECHARGE_SUCCESS = "WalletRechargeSuccess";
    String TAG_WALLET_WITHDRAW_SUCCESS = "WalletWithdrawSuccess";

    String GROUP_PAYMENT_SUCCESS_CONSUMER = "GID_PAYMENT_SUCCESS_CONFIRM_CONSUMER";

    /* ============================================================
     *  担保域（TCC / Outbox）
     * ============================================================ */
    String TOPIC_ESCROW_EVENTS = "ESCROW_EVENTS_TOPIC";
    String TAG_ESCROW_FROZEN_OK    = "EscrowFrozenConfirmed";
    String TAG_ESCROW_SETTLED_OK   = "EscrowSettled";   // 确认放款
    String TAG_ESCROW_REFUND_OK    = "EscrowRefunded";
    String TAG_ESCROW_UNFROZEN     = "EscrowUnfrozen";  // 订单取消解冻

    String GROUP_ESCROW_OUTBOX_CONSUMER = "GID_ESCROW_OUTBOX_SCAN_CONSUMER";

    /* ============================================================
     *  物流域
     * ============================================================ */
    String TOPIC_LOGISTICS_EVENTS = "LOGISTICS_EVENTS_TOPIC";
    String TAG_SHIPMENT_CREATED   = "ShipmentCreated";
    String TAG_LOGISTICS_UPDATED  = "LogisticsTrackUpdated";
    String TAG_LOGISTICS_DELIVERED = "LogisticsDelivered";

    /* ============================================================
     *  售后域
     * ============================================================ */
    String TOPIC_AFTERSALES_EVENTS = "AFTERSALES_EVENTS_TOPIC";
    String TAG_AFTERSALES_CREATED   = "AftersalesCreated";
    String TAG_AFTERSALES_APPROVED  = "AftersalesApproved";
    String TAG_AFTERSALES_REJECTED  = "AftersalesRejected";
    String TAG_AFTERSALES_FINISHED  = "AftersalesFinished";
    String TAG_ARBITRATION_AWARDED  = "ArbitrationAwarded"; // 仲裁裁决

    /* ============================================================
     *  消息域
     * ============================================================ */
    String TOPIC_NOTIFY_EVENTS = "NOTIFY_EVENTS_TOPIC";
    String TAG_NOTIFY_SITE  = "SiteMessage";      // 站内信
    String TAG_NOTIFY_PUSH  = "AppPush";          // App推送
    String TAG_NOTIFY_SMS   = "SmsSend";          // 短信

    /* ============================================================
     *  Canal Binlog → RocketMQ  → ES 同步
     * ============================================================ */
    String TOPIC_CANAL_PRODUCT_SYNC = "CANAL_PRODUCT_BINLOG_TOPIC";

    /* -------- Delay 消息等级（RocketMQ 默认等级） -------- */
    /** 1s/5s/10s/30s/1m/2m/3m/4m/5m/6m/7m/8m/9m/10m/20m/30m/1h/2h */
    int LEVEL_1M = 5;
    int LEVEL_5M = 9;
    int LEVEL_10M = 14;
    int LEVEL_30M = 16;
    int LEVEL_1H  = 17;
    int LEVEL_2H  = 18;
}
