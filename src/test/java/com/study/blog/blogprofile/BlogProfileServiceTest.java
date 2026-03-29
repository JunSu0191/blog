package com.study.blog.blogprofile;

import com.study.blog.core.exception.CodedApiException;
import com.study.blog.post.PostApplicationService;
import com.study.blog.post.PostRepository;
import com.study.blog.post.PostStatus;
import com.study.blog.post.dto.PostContractDto;
import com.study.blog.blogprofile.dto.BlogSettingsDto;
import com.study.blog.user.User;
import com.study.blog.user.UserProfile;
import com.study.blog.user.UserProfileRepository;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostApplicationService postApplicationService;
    @Mock
    private BlogSettingsService blogSettingsService;

    private BlogProfileService blogProfileService;

    @BeforeEach
    void setUp() {
        blogProfileService = new BlogProfileService(
                userRepository,
                userProfileRepository,
                postRepository,
                postApplicationService,
                blogSettingsService,
                "https://blog.example.com/");
    }

    @Test
    void getPublicProfileShouldReturnUserAndPosts() {
        User user = User.builder()
                .id(1L)
                .username("sample_author")
                .name("테스트작성자")
                .nickname("sample-nick")
                .deletedYn("N")
                .createdAt(LocalDateTime.of(2026, 3, 1, 12, 0))
                .build();
        UserProfile profile = UserProfile.builder()
                .id(10L)
                .user(user)
                .displayName("Sample Blog")
                .bio("백엔드 개발 기록")
                .avatarUrl("https://cdn.example.com/avatar.jpg")
                .websiteUrl("https://blog.example.com/sample_author")
                .location("Seoul")
                .build();

        PostContractDto.PostListItem post = new PostContractDto.PostListItem(
                100L,
                "첫 글",
                null,
                "요약",
                null,
                null,
                null,
                null,
                1L,
                "테스트작성자",
                List.of(),
                0L,
                0L,
                1,
                LocalDateTime.of(2026, 3, 2, 9, 0),
                new PostContractDto.AuthorSummary(1L, "sample_author", "테스트작성자", "sample-nick", "https://cdn.example.com/avatar.jpg"),
                List.of("https://cdn.example.com/first-image.jpg"));
        Page<PostContractDto.PostListItem> postPage = new PageImpl<>(List.of(post), PageRequest.of(0, 10), 1);

        when(userRepository.findByUsernameAndDeletedYn("sample_author", "N"))
                .thenReturn(Optional.of(user));
        when(userProfileRepository.findByUser_Id(1L))
                .thenReturn(Optional.of(profile));
        when(postApplicationService.listPublishedPostsByAuthor(1L, null, "latest", PageRequest.of(0, 10)))
                .thenReturn(postPage);
        when(postRepository.countByUser_IdAndDeletedYnAndDeletedAtIsNullAndStatusAndPublishedAtIsNotNull(
                1L, "N", PostStatus.PUBLISHED))
                .thenReturn(1L);
        when(blogSettingsService.getPublicSettings(1L))
                .thenReturn(new BlogSettingsDto.Response(
                        "minimal",
                        "#1D4ED8",
                        "https://cdn.example.com/cover.jpg",
                        "default",
                        "md",
                        true));

        var response = blogProfileService.getPublicProfile("sample_author", null, "latest", PageRequest.of(0, 10));

        assertThat(response.blogPath()).isEqualTo("/sample_author");
        assertThat(response.blogUrl()).isEqualTo("https://blog.example.com/sample_author");
        assertThat(response.user().username()).isEqualTo("sample_author");
        assertThat(response.profile().displayName()).isEqualTo("Sample Blog");
        assertThat(response.stats().publishedPostCount()).isEqualTo(1L);
        assertThat(response.blogSettings().accentColor()).isEqualTo("#1D4ED8");
        assertThat(response.posts().getContent()).hasSize(1);
    }

    @Test
    void getPublicProfileShouldThrowNotFoundWhenUserDoesNotExist() {
        when(userRepository.findByUsernameAndDeletedYn("unknown", "N"))
                .thenReturn(Optional.empty());

        CodedApiException ex = assertThrows(
                CodedApiException.class,
                () -> blogProfileService.getPublicProfile("unknown", null, "latest", PageRequest.of(0, 10)));

        assertThat(ex.getCode()).isEqualTo("blog_profile_not_found");
        assertThat(ex.getStatus().value()).isEqualTo(404);
    }
}
