package com.study.blog.like;

import com.study.blog.like.dto.LikeDto;
import com.study.blog.post.Post;
import com.study.blog.post.PostRepository;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;

    private PostLikeService postLikeService;

    @BeforeEach
    void setUp() {
        postLikeService = new PostLikeService(postLikeRepository, postRepository, userRepository);
    }

    @Test
    void likeShouldCreateNewLikeAndIncreaseCount() {
        User user = User.builder().id(1L).username("u1").name("U1").build();
        Post post = Post.builder().id(10L).user(user).deletedYn("N").likeCount(0L).build();

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.findByPost_IdAndUser_Id(10L, 1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(postLikeRepository.save(any(PostLike.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LikeDto.PostLikeResponse response = postLikeService.like(1L, 10L);

        assertThat(response.isLiked()).isTrue();
        assertThat(response.getLikeCount()).isEqualTo(1L);
        assertThat(post.getLikeCount()).isEqualTo(1L);
        verify(postRepository).save(post);
    }

    @Test
    void unlikeShouldSoftDeleteAndDecreaseCount() {
        User user = User.builder().id(1L).username("u1").name("U1").build();
        Post post = Post.builder().id(10L).user(user).deletedYn("N").likeCount(3L).build();
        PostLike existing = PostLike.builder().id(100L).post(post).user(user).deletedYn("N").build();

        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(postLikeRepository.findByPost_IdAndUser_Id(10L, 1L)).thenReturn(Optional.of(existing));
        when(postLikeRepository.save(any(PostLike.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LikeDto.PostLikeResponse response = postLikeService.unlike(1L, 10L);

        assertThat(response.isLiked()).isFalse();
        assertThat(response.getLikeCount()).isEqualTo(2L);
        assertThat(existing.getDeletedYn()).isEqualTo("Y");
        verify(postRepository).save(post);
    }
}
