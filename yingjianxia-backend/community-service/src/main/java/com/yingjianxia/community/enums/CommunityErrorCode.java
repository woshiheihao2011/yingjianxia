package com.yingjianxia.community.enums;

import com.yingjianxia.common.core.result.IErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 社区服务错误码（11001~11015）
 *
 * @author 硬件侠后端团队
 */
@Getter
@AllArgsConstructor
public enum CommunityErrorCode implements IErrorCode {
    POST_NOT_FOUND(11001, "帖子不存在"),
    POST_NO_PERMISSION(11002, "无权操作该帖子"),
    COMMENT_NOT_FOUND(11003, "评论不存在"),
    COMMENT_NO_PERMISSION(11004, "无权操作该评论"),
    LIKE_ALREADY_EXISTS(11005, "已点赞，不能重复点赞"),
    LIKE_NOT_EXISTS(11006, "尚未点赞，无法取消"),
    FOLLOW_ALREADY_EXISTS(11007, "已关注，不能重复关注"),
    FOLLOW_NOT_EXISTS(11008, "尚未关注，无法取关"),
    FOLLOW_SELF_NOT_ALLOWED(11009, "不能关注自己"),
    POST_CATEGORY_INVALID(11010, "帖子分类不合法（1~6）"),
    POST_STATUS_INVALID(11011, "帖子状态不合法"),
    POST_CONTENT_SENSITIVE(11012, "帖子内容含敏感词，已被拦截"),
    POST_HAS_COMMENTS(11013, "该帖子存在评论，无法删除"),
    PARENT_COMMENT_NOT_FOUND(11014, "父评论不存在"),
    POST_OFFLINE(11015, "该帖子已下架，不可评论或点赞");

    private final Integer code;
    private final String message;
}
