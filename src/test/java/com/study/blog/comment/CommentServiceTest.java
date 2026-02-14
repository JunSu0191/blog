package com.study.blog.comment;

import com.study.blog.comment.dto.CommentDto;
import com.study.blog.notification.NotificationService;
import com.study.blog.post.Post;
import com.study.blog.post.PostRepository;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, postRepository, userRepository, notificationService);
    }

    @Test
    void createCommentShouldTriggerPostOwnerNotification() {
        User owner = User.builder().id(1L).username("owner").name("Owner").build();
        User commenter = User.builder().id(2L).username("u2").name("Commenter").build();
        Post post = Post.builder().id(100L).user(owner).title("hello post").deletedYn("N").build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(commenter));
        when(postRepository.findById(100L)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setId(55L);
            return c;
        });

        CommentDto.CreateRequest req = new CommentDto.CreateRequest();
        req.postId = 100L;
        req.content = "new comment";

        CommentDto.Response response = commentService.createComment(req, 2L);

        assertThat(response.id).isEqualTo(55L);
        verify(notificationService).createPostCommentNotification(
                1L,
                2L,
                "Commenter",
                100L,
                "hello post",
                55L,
                null,
                "new comment");
    }

    @Test
    void updateCommentShouldTrimContentAndUpdateTimestamp() {
        User user = User.builder().id(1L).username("u1").name("U1").build();
        Post post = Post.builder().id(100L).user(user).title("t").deletedYn("N").build();
        Comment comment = Comment.builder()
                .id(10L)
                .post(post)
                .user(user)
                .content("before")
                .deletedYn("N")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        LocalDateTime beforeUpdatedAt = comment.getUpdatedAt();

        when(commentRepository.findByIdAndDeletedYn(10L, "N")).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentDto.UpdateRequest req = new CommentDto.UpdateRequest();
        req.content = "  edited content  ";

        CommentDto.Response response = commentService.updateComment(10L, req, 1L);

        assertThat(response.content).isEqualTo("edited content");
        assertThat(response.postId).isEqualTo(100L);
        assertThat(comment.getUpdatedAt()).isAfter(beforeUpdatedAt);
        verify(commentRepository).save(comment);
    }

    @Test
    void updateCommentShouldFailWhenNotOwner() {
        User owner = User.builder().id(1L).username("u1").name("U1").build();
        Post post = Post.builder().id(100L).user(owner).title("t").deletedYn("N").build();
        Comment comment = Comment.builder()
                .id(10L)
                .post(post)
                .user(owner)
                .content("before")
                .deletedYn("N")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(commentRepository.findByIdAndDeletedYn(10L, "N")).thenReturn(Optional.of(comment));

        CommentDto.UpdateRequest req = new CommentDto.UpdateRequest();
        req.content = "edited";

        assertThatThrownBy(() -> commentService.updateComment(10L, req, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("권한");
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void updateCommentShouldFailWhenCommentAlreadyDeleted() {
        when(commentRepository.findByIdAndDeletedYn(10L, "N")).thenReturn(Optional.empty());

        CommentDto.UpdateRequest req = new CommentDto.UpdateRequest();
        req.content = "edited";

        assertThatThrownBy(() -> commentService.updateComment(10L, req, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("댓글을 찾을 수 없습니다");
        verify(commentRepository, never()).save(any(Comment.class));
    }
}
