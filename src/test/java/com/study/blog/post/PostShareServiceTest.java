package com.study.blog.post;

import com.study.blog.core.web.PublicUrlBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PostShareServiceTest {

    private PostShareService postShareService;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        postShareService = new PostShareService(
                mock(PostRepository.class),
                mock(ScheduledPostPublicationService.class),
                new DefaultResourceLoader(),
                new PublicUrlBuilder("https://blog-pause.com"),
                "classpath:post-share-index.html",
                "https://blog-pause.com/assets/default-og.png");

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setScheme("https");
        mockRequest.setServerName("blog-pause.com");
        mockRequest.setServerPort(443);
        mockRequest.setRequestURI("/posts/123");
        request = mockRequest;
    }

    @Test
    void buildShareMetaShouldPreferSubtitleAndThumbnail() {
        Post post = publishedPost();
        post.setTitle("게시글 제목");
        post.setSubtitle("부제목 요약");
        post.setExcerpt("excerpt");
        post.setThumbnailUrl("https://cdn.blog-pause.com/thumb.jpg");

        PostShareService.ShareMeta meta = postShareService.buildShareMeta(post, request);

        assertThat(meta.pageTitle()).isEqualTo("게시글 제목 | blog-pause");
        assertThat(meta.ogTitle()).isEqualTo("게시글 제목");
        assertThat(meta.description()).isEqualTo("부제목 요약");
        assertThat(meta.imageUrl()).isEqualTo("https://cdn.blog-pause.com/thumb.jpg");
        assertThat(meta.canonicalUrl()).isEqualTo("https://blog-pause.com/posts/123");
    }

    @Test
    void buildDescriptionShouldFallbackToExcerptThenContent() {
        Post post = publishedPost();
        post.setSubtitle("   ");
        post.setExcerpt("excerpt summary");

        assertThat(postShareService.buildDescription(post)).isEqualTo("excerpt summary");

        post.setExcerpt(null);
        post.setContentHtml("<p>본문 첫 문장입니다.</p><p>두 번째 문장입니다.</p>");

        assertThat(postShareService.buildDescription(post)).isEqualTo("본문 첫 문장입니다. 두 번째 문장입니다.");
    }

    @Test
    void resolveImageUrlShouldConvertRelativeThumbnailAndFallbackToDefault() {
        Post post = publishedPost();
        post.setThumbnailUrl("/upload/thumb.png");

        assertThat(postShareService.resolveImageUrl(post, request))
                .isEqualTo("https://blog-pause.com/upload/thumb.png");

        post.setThumbnailUrl(null);

        assertThat(postShareService.resolveImageUrl(post, request))
                .isEqualTo("https://blog-pause.com/assets/default-og.png");
    }

    private Post publishedPost() {
        return Post.builder()
                .id(123L)
                .title("title")
                .slug("title")
                .status(PostStatus.PUBLISHED)
                .visibility(PostVisibility.PUBLIC)
                .deletedYn("N")
                .publishedAt(LocalDateTime.now())
                .build();
    }
}
