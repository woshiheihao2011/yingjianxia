package com.yingjianxia.community.service;

import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.community.dto.CommentCreateReq;
import com.yingjianxia.community.dto.PostCreateReq;
import com.yingjianxia.community.dto.PostQueryReq;
import com.yingjianxia.community.entity.CommunityPost;
import com.yingjianxia.community.entity.PostComment;
import com.yingjianxia.community.entity.UserFollow;

import java.util.List;

/**
 * 社区服务接口
 *
 * @author 硬件侠后端团队
 */
public interface CommunityService {

    /* ========== 帖子 ========== */

    /** 发帖（含敏感词过滤占位） */
    Long createPost(PostCreateReq req, Long authorId);

    /** 编辑帖子 */
    void editPost(Long postId, PostCreateReq req, Long authorId);

    /** 删除帖子 */
    void deletePost(Long postId, Long authorId);

    /** 帖子列表（按分类/热门/精选查询） */
    PageResult<CommunityPost> listPosts(PostQueryReq req);

    /** 帖子详情（浏览量 Redis 计数 + 异步回写） */
    CommunityPost getPostDetail(Long postId);

    /** 我的帖子列表 */
    PageResult<CommunityPost> myPosts(Long authorId, PostQueryReq req);

    /* ========== 评论 ========== */

    /** 评论/回复 */
    Long createComment(CommentCreateReq req, Long userId);

    /** 帖子评论列表 */
    List<PostComment> listComments(Long postId);

    /** 删除评论 */
    void deleteComment(Long commentId, Long userId);

    /* ========== 点赞 ========== */

    /** 点赞（唯一约束防重） */
    void likePost(Long postId, Long userId);

    /** 取消点赞 */
    void unlikePost(Long postId, Long userId);

    /* ========== 关注 ========== */

    /** 关注（唯一约束防重） */
    void follow(Long followerId, Long followingId);

    /** 取消关注 */
    void unfollow(Long followerId, Long followingId);

    /** 我的关注列表 */
    List<UserFollow> myFollowings(Long followerId);
}
