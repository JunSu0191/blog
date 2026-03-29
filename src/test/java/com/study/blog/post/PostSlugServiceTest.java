package com.study.blog.post;

import com.study.blog.core.exception.CodedApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostSlugServiceTest {

    @Mock
    private PostRepository postRepository;

    private PostSlugService postSlugService;

    @BeforeEach
    void setUp() {
        postSlugService = new PostSlugService(postRepository);
    }

    @Test
    void generateUniqueSlugShouldAppendSuffixWhenConflictExists() {
        when(postRepository.existsBySlug("hello-world")).thenReturn(true);
        when(postRepository.existsBySlug("hello-world-2")).thenReturn(false);

        String slug = postSlugService.generateUniqueSlug("Hello World", null);

        assertThat(slug).isEqualTo("hello-world-2");
    }

    @Test
    void generateUniqueSlugShouldKeepKoreanTitle() {
        when(postRepository.existsBySlug("한글-테스트")).thenReturn(false);

        String slug = postSlugService.generateUniqueSlug("한글 테스트", null);

        assertThat(slug).isEqualTo("한글-테스트");
    }

    @Test
    void generateUniqueSlugShouldThrowWhenTitleCannotBuildSlug() {
        assertThatThrownBy(() -> postSlugService.generateUniqueSlug("!!!", null))
                .isInstanceOf(CodedApiException.class)
                .hasMessageContaining("slug");
    }
}
