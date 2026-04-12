package com.study.blog.comment;

import com.study.blog.comment.dto.CommentDto;
import com.study.blog.notification.NotificationService;
import com.study.blog.post.Post;
import com.study.blog.post.PostRepository;
import com.study.blog.user.UserAvatarService;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 댓글 관련 비즈니스 로직 서비스 레이어입니다.
 */
@Slf4j
@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final UserAvatarService userAvatarService;

    public CommentService(CommentRepository commentRepository,
                          PostRepository postRepository,
                          UserRepository userRepository,
                          NotificationService notificationService,
                          UserAvatarService userAvatarService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.userAvatarService = userAvatarService;
    }

    /**
     * 특정 게시글의 루트 댓글 목록 조회
     */
    public List<CommentDto.Response> getCommentsByPostId(Long postId) {
        List<Comment> comments = commentRepository.findByPost_IdAndParentIsNullAndDeletedYnOrderByCreatedAtDesc(postId, "N");
        return toResponses(comments.stream()
                .filter(comment -> comment.getDeletedAt() == null)
                .collect(Collectors.toList()));
    }

    /**
     * 특정 부모 댓글의 답글 목록 조회
     */
    public List<CommentDto.Response> getRepliesByCommentId(Long commentId) {
        Comment parent = commentRepository.findByIdAndDeletedYn(commentId, "N")
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        return toResponses(commentRepository.findByParent_IdAndDeletedYnOrderByCreatedAtAsc(parent.getId(), "N").stream()
                .filter(reply -> reply.getDeletedAt() == null)
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .collect(Collectors.toList()));
    }

    /**
     * 댓글 생성
     */
    public CommentDto.Response createComment(CommentDto.CreateRequest req, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        Post post = postRepository.findById(req.postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + req.postId));

        Comment parent = null;
        if (req.parentId != null) {
            parent = commentRepository.findById(req.parentId)
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다: " + req.parentId));
        }

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .parent(parent)
                .content(req.content)
                .deletedYn("N")
                .deletedAt(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .likeCount(0L)
                .dislikeCount(0L)
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("Created comment: {}", saved.getId());

        Long parentId = parent != null ? parent.getId() : null;
        runAfterCommitOrNow(() -> notificationService.createPostCommentNotification(
                post.getUser().getId(),
                user.getId(),
                user.getName(),
                post.getId(),
                post.getTitle(),
                saved.getId(),
                parentId,
                saved.getContent()));

        return toResponse(saved, userAvatarService.getAvatarUrls(List.of(userId)));
    }

    /**
     * 댓글 수정
     */
    public CommentDto.Response updateComment(Long commentId, CommentDto.UpdateRequest req, Long userId) {
        Comment comment = commentRepository.findByIdAndDeletedYn(commentId, "N")
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        // 작성자 검증
        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("댓글 수정 권한이 없습니다.");
        }

        String normalizedContent = req.content != null ? req.content.trim() : "";
        if (normalizedContent.isEmpty()) {
            throw new IllegalArgumentException("댓글 내용은 비어 있을 수 없습니다.");
        }

        comment.setContent(normalizedContent);
        comment.setUpdatedAt(LocalDateTime.now());

        Comment saved = commentRepository.save(comment);
        return toResponse(saved, userAvatarService.getAvatarUrls(List.of(comment.getUser().getId())));
    }

    /**
     * 댓글 삭제 (소프트 삭제)
     */
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findByIdAndDeletedYn(commentId, "N")
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        // 작성자 검증
        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("댓글 삭제 권한이 없습니다.");
        }

        comment.setDeletedYn("Y");
        comment.setDeletedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        log.info("Deleted comment: {}", commentId);
    }

    /**
     * 특정 게시글의 댓글 수 조회
     */
    public Long getCommentCount(Long postId) {
        return commentRepository.countByPostIdAndDeletedYn(postId, "N");
    }

    /**
     * 특정 사용자가 작성한 댓글 수 조회
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Long getCommentCountByUser(Long userId) {
        return commentRepository.countByUser_IdAndDeletedYn(userId, "N");
    }

    /**
     * 특정 사용자가 작성한 댓글 목록 조회
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<CommentDto.Response> listByUser(Long userId) {
        return toResponses(commentRepository.findByUser_IdAndDeletedYnOrderByCreatedAtDesc(userId, "N").stream()
                .filter(comment -> comment.getDeletedAt() == null)
                .collect(Collectors.toList()));
    }

    private List<CommentDto.Response> toResponses(List<Comment> comments) {
        Map<Long, String> avatarUrls = userAvatarService.getAvatarUrls(comments.stream()
                .map(comment -> comment.getUser() != null ? comment.getUser().getId() : null)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet()));
        return comments.stream()
                .map(comment -> toResponse(comment, avatarUrls))
                .collect(Collectors.toList());
    }

    private CommentDto.Response toResponse(Comment comment, Map<Long, String> avatarUrls) {
        CommentDto.Response response = new CommentDto.Response();
        response.id = comment.getId();
        response.postId = comment.getPost().getId();
        response.userId = comment.getUser().getId();
        response.name = comment.getUser().getName();
        response.nickname = comment.getUser().getNickname();
        response.username = comment.getUser().getUsername();
        response.avatarUrl = avatarUrls.get(comment.getUser().getId());
        response.parentId = comment.getParent() != null ? comment.getParent().getId() : null;
        response.content = comment.getContent();
        response.deletedYn = comment.getDeletedYn();
        response.likeCount = comment.getLikeCount();
        response.dislikeCount = comment.getDislikeCount();
        response.myReaction = null;
        response.replyCount = commentRepository.countByParent_IdAndDeletedYn(comment.getId(), "N");
        response.createdAt = comment.getCreatedAt();
        response.updatedAt = comment.getUpdatedAt();
        return response;
    }

    private void runAfterCommitOrNow(Runnable runnable) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }
}
