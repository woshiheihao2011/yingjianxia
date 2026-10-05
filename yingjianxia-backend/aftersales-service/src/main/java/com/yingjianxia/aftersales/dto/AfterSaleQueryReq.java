package com.yingjianxia.aftersales.dto;

import com.yingjianxia.common.core.result.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 售后单分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "售后单查询请求")
public class AfterSaleQueryReq extends PageQuery {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "售后状态")
    private Integer status;

    @Schema(description = "售后类型")
    private Integer type;

    @Schema(description = "角色：1买家 2卖家 3客服")
    private Integer role;

    @Schema(description = "售后单号（模糊）")
    private String asNo;
}
