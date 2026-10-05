package com.yingjianxia.community.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.community.dto.CommentCreateReq;
import com.yingjianxia.community.dto.PostCreateReq;
import com.yingjianxia.community.dto.PostQueryReq;
import com.yingjianxia.community.entity.CommunityPost;
import com.yingjianxia.community.entity.PostComment;
import com.yingjianxia.community.entity.UserFollow;
import com.yingjianxia.community.service.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 社区服务 Controller
 *
 * @author 硬件侠后端团队
 */
@Tag(name = "社区服务", description = "帖子CRUD/列表/详情、评论CRUD、点赞/取消、关注/取关/我的关注")
@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService service;

    /* ========== 帖子 ========== */

    @Operation(summary = "发帖")
    @PostMapping("/posts")
    public ApiResponse<Long> createPost(@Valid @RequestBody PostCreateReq req) {
        return ApiResponse.success(service.createPost(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "编辑帖子")
    @PutMapping("/posts/{postId}")
    public ApiResponse<Void> editPost(@PathVariable Long postId, @Valid @RequestBody PostCreateReq req) {
        service.editPost(postId, req, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable Long postId) {
        service.deletePost(postId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "帖子列表（分类/热门/精选）")
    @GetMapping("/posts")
    public ApiResponse<PageResult<CommunityPost>> listPosts(PostQueryReq req) {
        return ApiResponse.success(service.listPosts(req));
    }

    @Operation(summary = "帖子详情")
    @GetMapping("/posts/{postId}")
    public ApiResponse<CommunityPost> getPost(@PathVariable Long postId) {
        return ApiResponse.success(service.getPostDetail(postId));
    }

    @Operation(summary = "我的帖子列表")
    @GetMapping("/posts/mine")
    public ApiResponse<PageResult<CommunityPost>> myPosts(PostQueryReq req) {
        return ApiResponse.success(service.myPosts(UserContext.requiredUserId(), req));
    }

    /* ========== 评论 ========== */

    @Operation(summary = "评论/回复")
    @PostMapping("/comments")
    public ApiResponse<Long> createComment(@Valid @RequestBody CommentCreateReq req) {
        return ApiResponse.success(service.createComment(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "对帖子发表评论（前端兼容路径）")
    @PostMapping("/posts/{postId}/comment")
    public ApiResponse<Long> commentPost(@PathVariable Long postId,
                                         @RequestBody java.util.Map<String, Object> body) {
        CommentCreateReq req = new CommentCreateReq();
        req.setPostId(postId);
        req.setContent(body != null && body.get("content") != null ? body.get("content").toString() : "");
        if (body != null && body.get("parentId") != null) {
            req.setParentId(Long.parseLong(body.get("parentId").toString()));
        }
        return ApiResponse.success(service.createComment(req, UserContext.requiredUserId()));
    }

    @Operation(summary = "帖子评论列表")
    @GetMapping("/comments/{postId}")
    public ApiResponse<List<PostComment>> listComments(@PathVariable Long postId) {
        return ApiResponse.success(service.listComments(postId));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {
        service.deleteComment(commentId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    /* ========== 点赞 ========== */

    @Operation(summary = "点赞")
    @PostMapping("/posts/{postId}/like")
    public ApiResponse<Void> likePost(@PathVariable Long postId) {
        service.likePost(postId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/posts/{postId}/like")
    public ApiResponse<Void> unlikePost(@PathVariable Long postId) {
        service.unlikePost(postId, UserContext.requiredUserId());
        return ApiResponse.success();
    }

    /* ========== 关注 ========== */

    @Operation(summary = "关注用户")
    @PostMapping("/follows/{followingId}")
    public ApiResponse<Void> follow(@PathVariable Long followingId) {
        service.follow(UserContext.requiredUserId(), followingId);
        return ApiResponse.success();
    }

    @Operation(summary = "取消关注")
    @DeleteMapping("/follows/{followingId}")
    public ApiResponse<Void> unfollow(@PathVariable Long followingId) {
        service.unfollow(UserContext.requiredUserId(), followingId);
        return ApiResponse.success();
    }

    @Operation(summary = "我的关注列表")
    @GetMapping("/follows/mine")
    public ApiResponse<List<UserFollow>> myFollowings() {
        return ApiResponse.success(service.myFollowings(UserContext.requiredUserId()));
    }

    @Operation(summary = "热搜话题列表")
    @GetMapping("/hot-topics")
    public ApiResponse<List<String>> hotTopics() {
        // TODO: 接入真实热搜算法（基于帖子热度、点赞数、浏览量等）
        return ApiResponse.success(List.of("RTX 4090实测", "iPhone 15 Pro体验", "二手显卡选购指南", "硬件验机避坑", "双11装机推荐"));
    }

    @Operation(summary = "社区排行榜")
    @GetMapping("/ranks")
    public ApiResponse<List<java.util.Map<String, Object>>> ranks() {
        // TODO: 接入真实排行数据（活跃用户、热门帖子等）
        return ApiResponse.success(List.of(
                java.util.Map.of("rank", 1, "userName", "硬件达人", "score", 9856),
                java.util.Map.of("rank", 2, "userName", "数码评测师", "score", 8721),
                java.util.Map.of("rank", 3, "userName", "二手老司机", "score", 7634)
        ));
    }
}
