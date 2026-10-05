package com.yingjianxia.message.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送消息请求
 */
@Data
@Schema(description = "发送消息请求")
public class SendMessageReq {

    @Schema(description = "接收者ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "接收者不能为空")
    private Long receiverId;

    @Schema(description = "关联商品ID（首次发起会话时携带）")
    private Long productId;

    @Schema(description = "消息类型：1文本 2图片", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "消息类型不能为空")
    private Integer msgType;

    @Schema(description = "消息内容/图片URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "消息内容不能为空")
    private String content;

    @Schema(description = "客户端消息ID（幂等防重）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "客户端消息ID不能为空")
    private String clientMsgId;
}
