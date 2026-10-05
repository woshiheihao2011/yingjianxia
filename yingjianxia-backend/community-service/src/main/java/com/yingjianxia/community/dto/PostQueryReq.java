package com.yingjianxia.community.dto;

import com.yingjianxia.common.core.result.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子列表查询请求
 * <p>
 * 排序：latest 最新发布 / hot 热门 / featured 精选
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "帖子列表查询")
public class PostQueryReq extends PageQuery {

    @Schema(description = "分类：1装机指南 2避坑攻略 3硬件评测 4二手验机 5问答互助 6晒单")
    private Integer category;

    @Schema(description = "关键词（标题/摘要模糊匹配）")
    private String keyword;

    @Schema(description = "排序：latest最新 hot热门 featured精选", defaultValue = "latest")
    private String sortMode;
}
