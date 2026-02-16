package com.study.blog.like;

import com.study.blog.comment.Comment;
import com.study.blog.comment.CommentRepository;
import com.study.blog.like.dto.LikeDto;
import com.study.blog.post.Post;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentReactionServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private UserRepository userRepository;

    private CommentReactionService commentReactionService;

    @BeforeEach
    void setUp() {
        commentReactionService = new CommentReactionService(commentRepository, commentLikeRepository, userRepository);
    }

    @Test
    void updateReactionShouldSwitchLikeToDislike() {
        User user = User.builder().id(1L).username("u1").name("U1").build();
        Post post = Post.builder().id(10L).user(user).deletedYn("N").build();
        Comment comment = Comment.builder()
                .id(20L)
                .post(post)
                .user(user)
                .deletedYn("N")
                .likeCount(5L)
                .dislikeCount(1L)
                .build();
        CommentLike existing = CommentLike.builder()
                .id(30L)
                .comment(comment)
                .user(user)
                .deletedYn("N")
                .reactionType(CommentReactionType.LIKE)
                .build();

        when(commentRepository.findById(20L)).thenReturn(Optional.of(comment));
        when(commentLikeRepository.findByComment_IdAndUser_Id(20L, 1L)).thenReturn(Optional.of(existing));
        when(commentLikeRepository.save(any(CommentLike.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LikeDto.CommentReactionResponse response = commentReactionService.updateReaction(1L, 20L, CommentReactionType.DISLIKE);

        assertThat(response.getMyReaction()).isEqualTo(CommentReactionType.DISLIKE);
        assertThat(response.getLikeCount()).isEqualTo(4L);
        assertThat(response.getDislikeCount()).isEqualTo(2L);
        assertThat(existing.getReactionType()).isEqualTo(CommentReactionType.DISLIKE);
        verify(commentRepository).save(comment);
    }

    @Test
    void updateReactionShouldCreateNewReactionWhenMissing() {
        User user = User.builder().id(1L).username("u1").name("U1").build();
        Post post = Post.builder().id(10L).user(user).deletedYn("N").build();
        Comment comment = Comment.builder()
                .id(21L)
                .post(post)
                .user(user)
                .deletedYn("N")
                .likeCount(0L)
                .dislikeCount(0L)
                .build();

        when(commentRepository.findById(21L)).thenReturn(Optional.of(comment));
        when(commentLikeRepository.findByComment_IdAndUser_Id(21L, 1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(commentLikeRepository.save(any(CommentLike.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LikeDto.CommentReactionResponse response = commentReactionService.updateReaction(1L, 21L, CommentReactionType.LIKE);

        assertThat(response.getMyReaction()).isEqualTo(CommentReactionType.LIKE);
        assertThat(response.getLikeCount()).isEqualTo(1L);
        assertThat(response.getDislikeCount()).isEqualTo(0L);
        verify(commentRepository).save(comment);
    }
}
