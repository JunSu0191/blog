package com.study.blog.admin;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ChatConversationRepository chatConversationRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                userRepository,
                postRepository,
                commentRepository,
                chatConversationRepository,
                notificationRepository,
                adminAuditLogRepository,
                new ObjectMapper());
    }

    @Test
    void updateUserRoleShouldRejectWhenDowngradingLastAdmin() {
        User actor = User.builder()
                .id(1L)
                .username("admin")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .deletedYn("N")
                .build();
        User target = User.builder()
                .id(2L)
                .username("admin2")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .deletedYn("N")
                .build();

        when(userRepository.findByUsernameAndDeletedYn("admin", "N")).thenReturn(Optional.of(actor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByRoleAndDeletedYn(UserRole.ADMIN, "N")).thenReturn(1L);

        assertThatThrownBy(() -> adminService.updateUserRole("admin", 2L, UserRole.USER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("최소 1명의 ADMIN");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void hidePostShouldSetDeletedAtAndDeletedYn() {
        User actor = User.builder()
                .id(1L)
                .username("admin")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .deletedYn("N")
                .build();
        User author = User.builder().id(2L).username("writer").name("writer").deletedYn("N").build();
        Post post = Post.builder()
                .id(10L)
                .user(author)
                .title("title")
                .content("content")
                .deletedYn("N")
                .deletedAt(null)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsernameAndDeletedYn("admin", "N")).thenReturn(Optional.of(actor));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        AdminDto.PostSummaryResponse response = adminService.hidePost("admin", 10L);

        assertThat(response.getDeletedYn()).isEqualTo("Y");
        assertThat(response.getDeletedAt()).isNotNull();
        assertThat(post.getDeletedYn()).isEqualTo("Y");
        assertThat(post.getDeletedAt()).isNotNull();
        verify(postRepository).save(post);
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }

    @Test
    void restoreCommentShouldSetDeletedAtNullAndDeletedYnN() {
        User actor = User.builder()
                .id(1L)
                .username("admin")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .deletedYn("N")
                .build();
        User author = User.builder().id(2L).username("writer").name("writer").deletedYn("N").build();
        Post post = Post.builder().id(3L).user(author).title("title").deletedYn("N").build();
        Comment comment = Comment.builder()
                .id(4L)
                .post(post)
                .user(author)
                .content("comment")
                .deletedYn("Y")
                .deletedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsernameAndDeletedYn("admin", "N")).thenReturn(Optional.of(actor));
        when(commentRepository.findById(4L)).thenReturn(Optional.of(comment));

        AdminDto.CommentSummaryResponse response = adminService.restoreComment("admin", 4L);

        assertThat(response.getPostTitle()).isEqualTo("title");
        assertThat(response.getDeletedYn()).isEqualTo("N");
        assertThat(response.getDeletedAt()).isNull();
        assertThat(comment.getDeletedYn()).isEqualTo("N");
        assertThat(comment.getDeletedAt()).isNull();
        verify(commentRepository).save(comment);
        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }
}
