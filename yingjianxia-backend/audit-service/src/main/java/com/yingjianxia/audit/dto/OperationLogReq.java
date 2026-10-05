package com.yingjianxia.audit.dto;

import com.yingjianxia.common.core.result.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志查询请求
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "操作日志查询")
public class OperationLogReq extends PageQuery {

    @Schema(description = "操作用户ID")
    private Long userId;

    @Schema(description = "模块")
    private String module;

    @Schema(description = "目标类型")
    private Integer targetType;

    @Schema(description = "目标对象ID")
    private Long targetId;
}
