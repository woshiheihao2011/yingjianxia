package com.yingjianxia.community.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.PageResult;
import com.yingjianxia.community.dto.CommentCreateReq;
import com.yingjianxia.community.dto.PostCreateReq;
import com.yingjianxia.community.dto.PostQueryReq;
import com.yingjianxia.community.entity.CommunityPost;
import com.yingjianxia.community.entity.PostComment;
import com.yingjianxia.community.entity.PostLike;
import com.yingjianxia.community.entity.UserFollow;
import com.yingjianxia.community.enums.CommunityErrorCode;
import com.yingjianxia.community.mapper.CommunityPostMapper;
import com.yingjianxia.community.mapper.PostCommentMapper;
import com.yingjianxia.community.mapper.PostLikeMapper;
import com.yingjianxia.community.mapper.UserFollowMapper;
import com.yingjianxia.community.service.CommunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 社区服务实现 — 帖子CRUD/评论/点赞/关注，浏览量 Redis 计数 + 定时回写
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final CommunityPostMapper postMapper;
    private final PostCommentMapper commentMapper;
    private final PostLikeMapper likeMapper;
    private final UserFollowMapper followMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    /** 浏览量 Redis Key：yjx:community:view:{postId} */
    private static final String VIEW_COUNT_KEY = "yjx:community:view:%s";
    /** 浏览量待回写帖子ID集合 */
    private static final String VIEW_DIRTY_SET = "yjx:community:view:dirty_ids";
    /** 批量回写每页大小 */
    private static final int FLUSH_BATCH = 200;

    /* ======================== 帖子 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(PostCreateReq req, Long authorId) {
        validateCategory(req.getCategory());
        // 敏感词过滤占位
        filterSensitive(req.getTitle());
        filterSensitive(req.getContent());

        CommunityPost post = new CommunityPost();
        post.setAuthorId(authorId);
        post.setCategory(req.getCategory());
        post.setTitle(req.getTitle());
        post.setContent(req.getContent());
        post.setCoverImage(req.getCoverImage());
        post.setImages(toJson(req.getImages()));
        post.setSummary(StrUtil.isBlank(req.getContent()) ? null
                : req.getContent().length() > 200 ? req.getContent().substring(0, 200) : req.getContent());
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setViewCount(0);
        post.setShareCount(0);
        post.setIsFeatured(0);
        post.setIsHot(0);
        post.setStatus(CommunityPost.STATUS_NORMAL);
        LocalDateTime now = LocalDateTime.now();
        post.setPublishedAt(now);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        postMapper.insert(post);
        log.info("【发帖】authorId={}, postId={}, category={}", authorId, post.getId(), req.getCategory());
        return post.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPost(Long postId, PostCreateReq req, Long authorId) {
        CommunityPost post = requirePost(postId);
        if (!post.getAuthorId().equals(authorId)) {
            throw new BusinessException(CommunityErrorCode.POST_NO_PERMISSION);
        }
        validateCategory(req.getCategory());
        filterSensitive(req.getTitle());
        filterSensitive(req.getContent());

        String summary = StrUtil.isBlank(req.getContent()) ? null
                : req.getContent().length() > 200 ? req.getContent().substring(0, 200) : req.getContent();
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .set(CommunityPost::getCategory, req.getCategory())
                .set(CommunityPost::getTitle, req.getTitle())
                .set(CommunityPost::getContent, req.getContent())
                .set(CommunityPost::getCoverImage, req.getCoverImage())
                .set(CommunityPost::getImages, toJson(req.getImages()))
                .set(CommunityPost::getSummary, summary)
                .set(CommunityPost::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId, Long authorId) {
        CommunityPost post = requirePost(postId);
        if (!post.getAuthorId().equals(authorId)) {
            throw new BusinessException(CommunityErrorCode.POST_NO_PERMISSION);
        }
        Long commentCnt = commentMapper.selectCount(new LambdaQueryWrapper<PostComment>()
                .eq(PostComment::getPostId, postId)
                .eq(PostComment::getStatus, PostComment.STATUS_NORMAL));
        if (commentCnt != null && commentCnt > 0) {
            throw new BusinessException(CommunityErrorCode.POST_HAS_COMMENTS);
        }
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .set(CommunityPost::getStatus, CommunityPost.STATUS_OFFLINE)
                .set(CommunityPost::getUpdatedAt, LocalDateTime.now()));
        log.info("【删帖】postId={}, authorId={}", postId, authorId);
    }

    @Override
    public PageResult<CommunityPost> listPosts(PostQueryReq req) {
        LambdaQueryWrapper<CommunityPost> qw = new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getStatus, CommunityPost.STATUS_NORMAL)
                .eq(req.getCategory() != null, CommunityPost::getCategory, req.getCategory())
                .and(StrUtil.isNotBlank(req.getKeyword()), w -> w
                        .like(CommunityPost::getTitle, req.getKeyword())
                        .or().like(CommunityPost::getSummary, req.getKeyword()));
        String sort = req.getSortMode() == null ? "latest" : req.getSortMode();
        switch (sort) {
            case "hot" -> qw.orderByDesc(CommunityPost::getIsHot)
                    .orderByDesc(CommunityPost::getLikeCount)
                    .orderByDesc(CommunityPost::getPublishedAt);
            case "featured" -> qw.eq(CommunityPost::getIsFeatured, 1)
                    .orderByDesc(CommunityPost::getPublishedAt);
            default -> qw.orderByDesc(CommunityPost::getPublishedAt);
        }
        IPage<CommunityPost> page = postMapper.selectPage(
                new Page<>(req.getPageNum(), req.getPageSize()), qw);
        page.getRecords().forEach(this::fillImages);
        return PageResult.of(req.getPageNum(), req.getPageSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public CommunityPost getPostDetail(Long postId) {
        CommunityPost post = requirePost(postId);
        fillImages(post);
        // 浏览量 Redis 计数（异步回写）
        incrViewCount(postId);
        return post;
    }

    @Override
    public PageResult<CommunityPost> myPosts(Long authorId, PostQueryReq req) {
        LambdaQueryWrapper<CommunityPost> qw = new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getAuthorId, authorId)
                .ne(CommunityPost::getStatus, CommunityPost.STATUS_VIOLATION)
                .like(StrUtil.isNotBlank(req.getKeyword()), CommunityPost::getTitle, req.getKeyword())
                .orderByDesc(CommunityPost::getPublishedAt);
        IPage<CommunityPost> page = postMapper.selectPage(
                new Page<>(req.getPageNum(), req.getPageSize()), qw);
        page.getRecords().forEach(this::fillImages);
        return PageResult.of(req.getPageNum(), req.getPageSize(), page.getTotal(), page.getRecords());
    }

    /* ======================== 评论 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(CommentCreateReq req, Long userId) {
        CommunityPost post = requirePost(req.getPostId());
        if (post.getStatus() != CommunityPost.STATUS_NORMAL) {
            throw new BusinessException(CommunityErrorCode.POST_OFFLINE);
        }
        Long replyToId = null;
        if (req.getParentId() != null) {
            PostComment parent = commentMapper.selectById(req.getParentId());
            if (parent == null || parent.getStatus() != PostComment.STATUS_NORMAL) {
                throw new BusinessException(CommunityErrorCode.PARENT_COMMENT_NOT_FOUND);
            }
            replyToId = parent.getUserId();
        }
        filterSensitive(req.getContent());
        PostComment comment = new PostComment();
        comment.setPostId(req.getPostId());
        comment.setUserId(userId);
        comment.setParentId(req.getParentId());
        comment.setReplyToId(replyToId);
        comment.setContent(req.getContent());
        comment.setLikeCount(0);
        comment.setStatus(PostComment.STATUS_NORMAL);
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        // 帖子评论数 +1
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, req.getPostId())
                .setSql("comment_count = comment_count + 1"));
        return comment.getId();
    }

    @Override
    public List<PostComment> listComments(Long postId) {
        return commentMapper.selectList(new LambdaQueryWrapper<PostComment>()
                .eq(PostComment::getPostId, postId)
                .eq(PostComment::getStatus, PostComment.STATUS_NORMAL)
                .orderByAsc(PostComment::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        PostComment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != PostComment.STATUS_NORMAL) {
            throw new BusinessException(CommunityErrorCode.COMMENT_NOT_FOUND);
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(CommunityErrorCode.COMMENT_NO_PERMISSION);
        }
        commentMapper.update(null, new LambdaUpdateWrapper<PostComment>()
                .eq(PostComment::getId, commentId)
                .set(PostComment::getStatus, PostComment.STATUS_DELETED));
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, comment.getPostId())
                .setSql("comment_count = GREATEST(comment_count - 1, 0)"));
    }

    /* ======================== 点赞 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likePost(Long postId, Long userId) {
        requirePost(postId);
        Long cnt = likeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(CommunityErrorCode.LIKE_ALREADY_EXISTS);
        }
        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        like.setCreatedAt(LocalDateTime.now());
        try {
            likeMapper.insert(like);
        } catch (Exception e) {
            // 唯一约束兜底
            throw new BusinessException(CommunityErrorCode.LIKE_ALREADY_EXISTS);
        }
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .setSql("like_count = like_count + 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikePost(Long postId, Long userId) {
        int rows = likeMapper.delete(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId));
        if (rows == 0) {
            throw new BusinessException(CommunityErrorCode.LIKE_NOT_EXISTS);
        }
        postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                .eq(CommunityPost::getId, postId)
                .setSql("like_count = GREATEST(like_count - 1, 0)"));
    }

    /* ======================== 关注 ======================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException(CommunityErrorCode.FOLLOW_SELF_NOT_ALLOWED);
        }
        Long cnt = followMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowingId, followingId));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(CommunityErrorCode.FOLLOW_ALREADY_EXISTS);
        }
        UserFollow follow = new UserFollow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        follow.setCreatedAt(LocalDateTime.now());
        try {
            followMapper.insert(follow);
        } catch (Exception e) {
            throw new BusinessException(CommunityErrorCode.FOLLOW_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long followerId, Long followingId) {
        int rows = followMapper.delete(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowingId, followingId));
        if (rows == 0) {
            throw new BusinessException(CommunityErrorCode.FOLLOW_NOT_EXISTS);
        }
    }

    @Override
    public List<UserFollow> myFollowings(Long followerId) {
        return followMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .orderByDesc(UserFollow::getCreatedAt));
    }

    /* ======================== 浏览量 Redis 计数 + 定时回写 ======================== */

    /** 浏览量 +1（Redis 计数，标记待回写） */
    private void incrViewCount(Long postId) {
        String key = String.format(VIEW_COUNT_KEY, postId);
        redisTemplate.opsForValue().increment(key);
        redisTemplate.opsForSet().add(VIEW_DIRTY_SET, String.valueOf(postId));
    }

    /** 每 2 分钟回写一次浏览量到 DB */
    @Scheduled(fixedDelay = 120_000L)
    public void flushViewCount() {
        Set<String> dirty = redisTemplate.opsForSet().members(VIEW_DIRTY_SET);
        if (CollUtil.isEmpty(dirty)) return;
        int processed = 0;
        for (String idStr : dirty) {
            if (processed >= FLUSH_BATCH) break;
            try {
                Long postId = Long.valueOf(idStr);
                String key = String.format(VIEW_COUNT_KEY, postId);
                String val = redisTemplate.opsForValue().get(key);
                long incr = val == null ? 0 : Long.parseLong(val);
                if (incr > 0) {
                    postMapper.update(null, new LambdaUpdateWrapper<CommunityPost>()
                            .eq(CommunityPost::getId, postId)
                            .setSql("view_count = view_count + " + incr));
                    redisTemplate.delete(key);
                }
                redisTemplate.opsForSet().remove(VIEW_DIRTY_SET, idStr);
                processed++;
            } catch (Exception e) {
                log.warn("回写浏览量失败 postId={}", idStr, e);
            }
        }
        if (processed > 0) {
            log.info("【浏览量回写】本次回写 {} 篇帖子", processed);
        }
    }

    /* ======================== 内部工具 ======================== */

    private CommunityPost requirePost(Long id) {
        CommunityPost post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(CommunityErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    private void validateCategory(Integer category) {
        if (category == null || category < 1 || category > 6) {
            throw new BusinessException(CommunityErrorCode.POST_CATEGORY_INVALID);
        }
    }

    /**
     * 敏感词过滤占位 — 生产环境对接风控服务/敏感词库 DFA 算法
     */
    private void filterSensitive(String text) {
        if (StrUtil.isBlank(text)) return;
        // TODO: 对接 risk-service 敏感词检测；此处仅占位，命中即拦截
    }

    private String toJson(List<String> images) {
        if (CollUtil.isEmpty(images)) return null;
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void fillImages(CommunityPost post) {
        if (StrUtil.isNotBlank(post.getImages())) {
            try {
                post.setImageList(objectMapper.readValue(post.getImages(), new TypeReference<List<String>>() {}));
            } catch (JsonProcessingException e) {
                post.setImageList(Collections.emptyList());
            }
        }
    }
}
