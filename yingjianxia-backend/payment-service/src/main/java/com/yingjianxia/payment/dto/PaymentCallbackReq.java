package com.yingjianxia.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付回调请求（微信/支付宝通用结构）
 * <p>
 * 第三方异步通知 payment-service 时，网关将原始报文解析为此结构。
 * 验签使用 sign 字段，幂等使用 paymentNo + channelOrderNo。
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "支付回调请求")
public class PaymentCallbackReq {

    @Schema(description = "第三方渠道订单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "渠道订单号不能为空")
    private String channelOrderNo;

    @Schema(description = "我方支付单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "支付单号不能为空")
    private String paymentNo;

    @Schema(description = "支付金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "支付金额不能为空")
    private BigDecimal amount;

    @Schema(description = "签名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "签名不能为空")
    private String sign;

    @Schema(description = "时间戳（秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "时间戳不能为空")
    private Long timestamp;

    @Schema(description = "原始回调数据（JSON，审计追溯用）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原始回调数据不能为空")
    private String rawData;

    @Schema(description = "支付渠道：wechat / alipay")
    private String channel;
}
