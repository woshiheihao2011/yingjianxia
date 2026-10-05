package com.yingjianxia.escrow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 交易流水查询条件
 */
@Data
@Schema(description = "交易流水查询条件")
public class TransactionQueryReq {

    @Schema(description = "流水号（精确匹配）")
    private String txNo;

    @Schema(description = "类型：1收入 2支出 3退款 4充值 5提现 6冻结 7解冻")
    private Integer type;

    @Schema(description = "关联订单ID")
    private Long orderId;

    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", defaultValue = "20")
    private Integer pageSize = 20;
}
