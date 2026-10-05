package com.yingjianxia.support.dto;

import com.yingjianxia.common.core.result.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公告查询请求
 *
 * @author 硬件侠后端团队
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "公告查询")
public class AnnouncementQueryReq extends PageQuery {

    @Schema(description = "分类：1平台公告 2活动通知 3政策更新 4系统维护")
    private Integer category;

    @Schema(description = "关键词（标题模糊匹配）")
    private String keyword;
}
