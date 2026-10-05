package com.yingjianxia.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款回调请求
 * <p>
 * 第三方退款成功后异步通知本服务。
 *
 * @author 硬件侠后端团队
 */
@Data
@Schema(description = "退款回调请求")
public class RefundCallbackReq {

    @Schema(description = "第三方渠道退款单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "渠道退款单号不能为空")
    private String channelRefundNo;

    @Schema(description = "我方支付单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "支付单号不能为空")
    private String paymentNo;

    @Schema(description = "退款金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "退款金额不能为空")
    private BigDecimal refundAmount;

    @Schema(description = "签名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "签名不能为空")
    private String sign;

    @Schema(description = "时间戳（秒）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "时间戳不能为空")
    private Long timestamp;

    @Schema(description = "原始回调数据（JSON）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "原始回调数据不能为空")
    private String rawData;

    @Schema(description = "支付渠道：wechat / alipay")
    private String channel;
}
