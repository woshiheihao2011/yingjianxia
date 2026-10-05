package com.yingjianxia.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 购物车更新请求
 */
@Data
@Schema(description = "购物车更新")
public class CartUpdateReq {

    @Schema(description = "购物车项ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "购物车项ID不能为空")
    private Long id;

    @Schema(description = "数量")
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;

    @Schema(description = "是否勾选 1是 0否")
    private Integer isSelected;
}
