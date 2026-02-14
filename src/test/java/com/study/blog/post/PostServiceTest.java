package com.study.blog.post;

import com.study.blog.core.response.CursorResponse;
import com.study.blog.post.dto.PostDto;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;

    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, userRepository);
    }

    @Test
    void listPostsShouldUseDefaultListWhenKeywordIsBlank() {
        User user = User.builder().id(1L).username("u1").name("U1").build();
        Post post = Post.builder()
                .id(10L)
                .user(user)
                .title("title")
                .content("content")
                .deletedYn("N")
                .createdAt(LocalDateTime.now())
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);

        when(postRepository.findByDeletedYn("N", pageable)).thenReturn(page);

        Page<PostDto.Response> result = postService.listPosts(pageable, "   ");

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id).isEqualTo(10L);
        verify(postRepository).findByDeletedYn("N", pageable);
        verify(postRepository, never())
                .findByDeletedYnAndTitleContainingIgnoreCaseOrDeletedYnAndContentContainingIgnoreCase(
                        any(), any(), any(), any(), any());
    }

    @Test
    void listPostsShouldUseSearchWhenKeywordExists() {
        User user = User.builder().id(1L).username("u1").name("U1").build();
        Post post = Post.builder()
                .id(11L)
                .user(user)
                .title("hello title")
                .content("body")
                .deletedYn("N")
                .createdAt(LocalDateTime.now())
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(post), pageable, 1);

        when(postRepository.findByDeletedYnAndTitleContainingIgnoreCaseOrDeletedYnAndContentContainingIgnoreCase(
                "N", "hello", "N", "hello", pageable)).thenReturn(page);

        Page<PostDto.Response> result = postService.listPosts(pageable, " hello ");

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).title).isEqualTo("hello title");
        verify(postRepository).findByDeletedYnAndTitleContainingIgnoreCaseOrDeletedYnAndContentContainingIgnoreCase(
                eq("N"), eq("hello"), eq("N"), eq("hello"), eq(pageable));
        verify(postRepository, never()).findByDeletedYn("N", pageable);
    }

    @Test
    void listPostsByCursorShouldReturnNextCursorWhenHasNext() {
        User user = User.builder().id(1L).username("u1").name("U1").build();
        Post post31 = Post.builder().id(31L).user(user).title("t31").content("c").deletedYn("N").createdAt(LocalDateTime.now()).build();
        Post post30 = Post.builder().id(30L).user(user).title("t30").content("c").deletedYn("N").createdAt(LocalDateTime.now()).build();
        Post post29 = Post.builder().id(29L).user(user).title("t29").content("c").deletedYn("N").createdAt(LocalDateTime.now()).build();

        when(postRepository.findCursorPageWithoutKeyword(eq("N"), eq(40L), any(Pageable.class)))
                .thenReturn(List.of(post31, post30, post29));

        CursorResponse<PostDto.Response> result = postService.listPostsByCursor(40L, 2, null);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).id).isEqualTo(31L);
        assertThat(result.getContent().get(1).id).isEqualTo(30L);
        assertThat(result.isHasNext()).isTrue();
        assertThat(result.getNextCursorId()).isEqualTo(30L);
    }

    @Test
    void listPostsByCursorShouldUseKeywordQueryWhenKeywordExists() {
        User user = User.builder().id(1L).username("u1").name("U1").build();
        Post post = Post.builder().id(21L).user(user).title("hello title").content("body").deletedYn("N")
                .createdAt(LocalDateTime.now()).build();

        when(postRepository.findCursorPageWithKeyword(eq("N"), eq("hello"), isNull(), any(Pageable.class)))
                .thenReturn(List.of(post));

        CursorResponse<PostDto.Response> result = postService.listPostsByCursor(null, 10, " hello ");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id).isEqualTo(21L);
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getNextCursorId()).isNull();
        verify(postRepository, never()).findCursorPageWithoutKeyword(any(), any(), any());
    }
}
