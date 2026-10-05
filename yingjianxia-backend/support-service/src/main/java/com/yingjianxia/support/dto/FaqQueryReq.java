package com.yingjianxia.support.dto;

import com.yingjianxia.common.core.result.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * FAQ查询请求
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "FAQ查询")
public class FaqQueryReq extends PageQuery {

    @Schema(description = "分类：账号/交易/物流/售后/...")
    private String category;

    @Schema(description = "关键词（问题模糊匹配）")
    private String keyword;
}
