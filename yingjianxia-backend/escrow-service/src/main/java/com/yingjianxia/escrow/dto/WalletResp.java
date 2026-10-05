package com.yingjianxia.escrow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包信息响应
 */
@Data
@Schema(description = "钱包信息")
public class WalletResp {

    @Schema(description = "钱包ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "总余额")
    private BigDecimal totalBalance;

    @Schema(description = "可用余额")
    private BigDecimal availableBalance;

    @Schema(description = "冻结金额")
    private BigDecimal frozenBalance;

    @Schema(description = "最后一笔流水号")
    private String lastTxNo;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
