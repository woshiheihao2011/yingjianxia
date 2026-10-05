package com.yingjianxia.audit.dto;

import com.yingjianxia.common.core.result.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审核记录查询请求
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "审核记录查询")
public class AuditQueryReq extends PageQuery {

    @Schema(description = "目标类型：1商品 2帖子 3举报 4提现")
    private Integer targetType;

    @Schema(description = "目标对象ID")
    private Long targetId;

    @Schema(description = "状态：0待审核 1已通过 2已拒绝")
    private Integer status;

    @Schema(description = "审核员ID")
    private Long auditorId;
}
