package com.yingjianxia.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 订单查询条件（买家/卖家/管理端通用）
 */
@Data
@Schema(description = "订单查询条件")
public class OrderQueryReq {

    @Schema(description = "订单号（精确匹配）")
    private String orderNo;

    @Schema(description = "订单状态：0待付款 1待发货 2待收货 3已完成 4已取消 5售后中")
    private Integer status;

    @Schema(description = "买家ID（管理端用）")
    private Long buyerId;

    @Schema(description = "卖家ID（管理端用）")
    private Long sellerId;

    @Schema(description = "角色：buyer买家 seller卖家")
    private String role;

    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", defaultValue = "10")
    private Integer pageSize = 10;
}
