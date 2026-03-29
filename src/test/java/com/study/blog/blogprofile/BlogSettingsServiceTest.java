package com.study.blog.blogprofile;

import com.study.blog.blogprofile.dto.BlogSettingsDto;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogSettingsServiceTest {

    @Mock
    private BlogSettingsRepository blogSettingsRepository;
    @Mock
    private UserRepository userRepository;

    private BlogSettingsService blogSettingsService;

    @BeforeEach
    void setUp() {
        blogSettingsService = new BlogSettingsService(blogSettingsRepository, userRepository);
    }

    @Test
    void getMySettingsShouldReturnDefaultWhenMissing() {
        User user = User.builder().id(1L).username("writer").deletedYn("N").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(blogSettingsRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        BlogSettingsDto.Response response = blogSettingsService.getMySettings(1L);

        assertThat(response.themePreset()).isEqualTo("minimal");
        assertThat(response.accentColor()).isEqualTo("#2563eb");
        assertThat(response.profileLayout()).isEqualTo("default");
        assertThat(response.fontScale()).isEqualTo("md");
        assertThat(response.showStats()).isTrue();
    }

    @Test
    void upsertMySettingsShouldPersistNormalizedValues() {
        User user = User.builder().id(1L).username("writer").deletedYn("N").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(blogSettingsRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        when(blogSettingsRepository.save(any(BlogSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BlogSettingsDto.UpdateRequest request = new BlogSettingsDto.UpdateRequest();
        request.setThemePreset("minimal");
        request.setAccentColor("#1D4ED8");
        request.setCoverImageUrl("https://example.com/cover.png");
        request.setProfileLayout("compact");
        request.setFontScale("lg");
        request.setShowStats(false);

        BlogSettingsDto.Response response = blogSettingsService.upsertMySettings(1L, request);

        assertThat(response.themePreset()).isEqualTo("minimal");
        assertThat(response.accentColor()).isEqualTo("#1D4ED8");
        assertThat(response.coverImageUrl()).isEqualTo("https://example.com/cover.png");
        assertThat(response.profileLayout()).isEqualTo("compact");
        assertThat(response.fontScale()).isEqualTo("lg");
        assertThat(response.showStats()).isFalse();
    }

    @Test
    void upsertMySettingsShouldRejectInvalidThemePreset() {
        User user = User.builder().id(1L).username("writer").deletedYn("N").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(blogSettingsRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        BlogSettingsDto.UpdateRequest request = new BlogSettingsDto.UpdateRequest();
        request.setThemePreset("neon");

        assertThatThrownBy(() -> blogSettingsService.upsertMySettings(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("themePreset");
    }

    @Test
    void upsertMySettingsShouldRejectInvalidAccentColor() {
        User user = User.builder().id(1L).username("writer").deletedYn("N").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(blogSettingsRepository.findByUser_Id(1L)).thenReturn(Optional.empty());

        BlogSettingsDto.UpdateRequest request = new BlogSettingsDto.UpdateRequest();
        request.setAccentColor("#12345");

        assertThatThrownBy(() -> blogSettingsService.upsertMySettings(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accentColor");
    }

    @Test
    void upsertMySettingsShouldStoreNullCoverImageWhenBlank() {
        User user = User.builder().id(1L).username("writer").deletedYn("N").build();
        BlogSettings existing = BlogSettings.builder()
                .user(user)
                .themePreset("minimal")
                .accentColor("#2563eb")
                .profileLayout("default")
                .fontScale("md")
                .showStats(true)
                .coverImageUrl("https://example.com/old-cover.png")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(blogSettingsRepository.findByUser_Id(1L)).thenReturn(Optional.of(existing));
        when(blogSettingsRepository.save(any(BlogSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BlogSettingsDto.UpdateRequest request = new BlogSettingsDto.UpdateRequest();
        request.setCoverImageUrl("   ");

        BlogSettingsDto.Response response = blogSettingsService.upsertMySettings(1L, request);

        assertThat(response.coverImageUrl()).isNull();
    }
}
