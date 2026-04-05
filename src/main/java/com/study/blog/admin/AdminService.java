package com.study.blog.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.blog.admin.dto.AdminDto;
import com.study.blog.chat.ChatConversationRepository;
import com.study.blog.comment.Comment;
import com.study.blog.comment.CommentRepository;
import com.study.blog.notification.NotificationRepository;
import com.study.blog.post.Post;
import com.study.blog.post.PostRepository;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import com.study.blog.user.UserRole;
import com.study.blog.user.UserStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final NotificationRepository notificationRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final ObjectMapper objectMapper;

    public AdminService(UserRepository userRepository,
                        PostRepository postRepository,
                        CommentRepository commentRepository,
                        ChatConversationRepository chatConversationRepository,
                        NotificationRepository notificationRepository,
                        AdminAuditLogRepository adminAuditLogRepository,
                        ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.chatConversationRepository = chatConversationRepository;
        this.notificationRepository = notificationRepository;
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.objectMapper = objectMapper;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AdminDto.MeResponse getMe(String actorUsername) {
        User admin = getActorAdminOrThrow(actorUsername);
        return toMeResponse(admin);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AdminDto.DashboardSummaryResponse getDashboardSummary() {
        AdminDto.DashboardSummaryResponse response = new AdminDto.DashboardSummaryResponse();
        response.setTotalUsers(userRepository.countByDeletedYn("N"));
        response.setTotalPosts(postRepository.countByDeletedAtIsNull());
        response.setTotalComments(commentRepository.countByDeletedAtIsNull());
        response.setTotalConversations(chatConversationRepository.count());
        response.setTotalNotifications(notificationRepository.countByArchivedAtIsNull());
        return response;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<AdminDto.UserSummaryResponse> listUsers(Pageable pageable, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return userRepository.searchAdminUsers(normalizedKeyword, pageable)
                .map(this::toUserSummaryResponse);
    }

    public AdminDto.UserSummaryResponse updateUserRole(String actorUsername, Long targetUserId, UserRole targetRole) {
        User actor = getActorAdminOrThrow(actorUsername);
        User target = getUserOrThrow(targetUserId);

        if (target.getRole() == UserRole.ADMIN && targetRole == UserRole.USER) {
            long adminCount = userRepository.countByRoleAndDeletedYn(UserRole.ADMIN, "N");
            if (adminCount <= 1) {
                throw new IllegalStateException("최소 1명의 ADMIN은 유지되어야 합니다.");
            }
        }

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("role", target.getRole().name());

        target.setRole(targetRole);
        userRepository.save(target);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("role", target.getRole().name());
        saveAudit(actor.getId(), "USER_ROLE_UPDATED", "USER", target.getId(), before, after);

        return toUserSummaryResponse(target);
    }

    public AdminDto.UserSummaryResponse updateUserStatus(String actorUsername, Long targetUserId, UserStatus targetStatus) {
        User actor = getActorAdminOrThrow(actorUsername);
        User target = getUserOrThrow(targetUserId);

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("status", target.getStatus().name());

        target.setStatus(targetStatus);
        userRepository.save(target);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", target.getStatus().name());
        saveAudit(actor.getId(), "USER_STATUS_UPDATED", "USER", target.getId(), before, after);

        return toUserSummaryResponse(target);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<AdminDto.PostSummaryResponse> listPosts(Pageable pageable, String keyword, Boolean deleted) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return postRepository.searchAdminPosts(normalizedKeyword, deleted, pageable)
                .map(this::toPostSummaryResponse);
    }

    public AdminDto.PostSummaryResponse hidePost(String actorUsername, Long postId) {
        User actor = getActorAdminOrThrow(actorUsername);
        Post post = getPostOrThrow(postId);
        Map<String, Object> before = snapshotDeleteState(post.getDeletedYn(), post.getDeletedAt());

        post.setDeletedYn("Y");
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);

        Map<String, Object> after = snapshotDeleteState(post.getDeletedYn(), post.getDeletedAt());
        saveAudit(actor.getId(), "POST_HIDDEN", "POST", post.getId(), before, after);
        return toPostSummaryResponse(post);
    }

    public AdminDto.PostSummaryResponse restorePost(String actorUsername, Long postId) {
        User actor = getActorAdminOrThrow(actorUsername);
        Post post = getPostOrThrow(postId);
        Map<String, Object> before = snapshotDeleteState(post.getDeletedYn(), post.getDeletedAt());

        post.setDeletedYn("N");
        post.setDeletedAt(null);
        postRepository.save(post);

        Map<String, Object> after = snapshotDeleteState(post.getDeletedYn(), post.getDeletedAt());
        saveAudit(actor.getId(), "POST_RESTORED", "POST", post.getId(), before, after);
        return toPostSummaryResponse(post);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<AdminDto.CommentSummaryResponse> listComments(Pageable pageable, String keyword, Boolean deleted) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return commentRepository.searchAdminComments(normalizedKeyword, deleted, pageable)
                .map(this::toCommentSummaryResponse);
    }

    public AdminDto.CommentSummaryResponse hideComment(String actorUsername, Long commentId) {
        User actor = getActorAdminOrThrow(actorUsername);
        Comment comment = getCommentOrThrow(commentId);
        Map<String, Object> before = snapshotDeleteState(comment.getDeletedYn(), comment.getDeletedAt());

        comment.setDeletedYn("Y");
        comment.setDeletedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        Map<String, Object> after = snapshotDeleteState(comment.getDeletedYn(), comment.getDeletedAt());
        saveAudit(actor.getId(), "COMMENT_HIDDEN", "COMMENT", comment.getId(), before, after);
        return toCommentSummaryResponse(comment);
    }

    public AdminDto.CommentSummaryResponse restoreComment(String actorUsername, Long commentId) {
        User actor = getActorAdminOrThrow(actorUsername);
        Comment comment = getCommentOrThrow(commentId);
        Map<String, Object> before = snapshotDeleteState(comment.getDeletedYn(), comment.getDeletedAt());

        comment.setDeletedYn("N");
        comment.setDeletedAt(null);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        Map<String, Object> after = snapshotDeleteState(comment.getDeletedYn(), comment.getDeletedAt());
        saveAudit(actor.getId(), "COMMENT_RESTORED", "COMMENT", comment.getId(), before, after);
        return toCommentSummaryResponse(comment);
    }

    private User getActorAdminOrThrow(String actorUsername) {
        User user = userRepository.findByUsernameAndDeletedYn(actorUsername, "N")
                .orElseThrow(() -> new IllegalArgumentException("관리자 사용자를 찾을 수 없습니다."));
        if (user.getRole() != UserRole.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException("관리자 권한이 필요합니다.");
        }
        return user;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> "N".equalsIgnoreCase(user.getDeletedYn()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    private Post getPostOrThrow(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));
    }

    private Comment getCommentOrThrow(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void saveAudit(Long actorAdminId,
                           String action,
                           String targetType,
                           Long targetId,
                           Object before,
                           Object after) {
        adminAuditLogRepository.save(AdminAuditLog.builder()
                .actorAdminId(actorAdminId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .beforeJson(toJson(before))
                .afterJson(toJson(after))
                .build());
    }

    private String toJson(Object source) {
        try {
            return source == null ? null : objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException ex) {
            return source == null ? null : source.toString();
        }
    }

    private Map<String, Object> snapshotDeleteState(String deletedYn, LocalDateTime deletedAt) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("deletedYn", deletedYn);
        state.put("deletedAt", deletedAt);
        return state;
    }

    private AdminDto.MeResponse toMeResponse(User user) {
        AdminDto.MeResponse response = new AdminDto.MeResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setName(user.getName());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setMustChangePassword(user.getMustChangePassword());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    private AdminDto.UserSummaryResponse toUserSummaryResponse(User user) {
        AdminDto.UserSummaryResponse response = new AdminDto.UserSummaryResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setName(user.getName());
        response.setNickname(user.getNickname());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setMustChangePassword(user.getMustChangePassword());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    private AdminDto.PostSummaryResponse toPostSummaryResponse(Post post) {
        AdminDto.PostSummaryResponse response = new AdminDto.PostSummaryResponse();
        response.setId(post.getId());
        response.setUserId(post.getUser().getId());
        response.setUsername(post.getUser().getUsername());
        response.setTitle(post.getTitle());
        response.setDeletedYn(post.getDeletedYn());
        response.setCreatedAt(post.getCreatedAt());
        response.setDeletedAt(post.getDeletedAt());
        return response;
    }

    private AdminDto.CommentSummaryResponse toCommentSummaryResponse(Comment comment) {
        AdminDto.CommentSummaryResponse response = new AdminDto.CommentSummaryResponse();
        response.setId(comment.getId());
        response.setPostId(comment.getPost().getId());
        response.setPostTitle(comment.getPost().getTitle());
        response.setUserId(comment.getUser().getId());
        response.setUsername(comment.getUser().getUsername());
        response.setContent(comment.getContent());
        response.setDeletedYn(comment.getDeletedYn());
        response.setCreatedAt(comment.getCreatedAt());
        response.setDeletedAt(comment.getDeletedAt());
        return response;
    }
}
