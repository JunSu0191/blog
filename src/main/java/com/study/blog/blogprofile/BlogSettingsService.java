package com.study.blog.blogprofile;

import com.study.blog.blogprofile.dto.BlogSettingsDto;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional
public class BlogSettingsService {

    private static final String FLAG_NO = "N";
    private static final Set<String> ALLOWED_THEME_PRESETS = Set.of("minimal", "ocean", "sunset", "forest");
    private static final Set<String> ALLOWED_PROFILE_LAYOUTS = Set.of("default", "compact", "centered");
    private static final Set<String> ALLOWED_FONT_SCALES = Set.of("sm", "md", "lg");
    private static final Pattern ACCENT_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final BlogSettingsRepository blogSettingsRepository;
    private final UserRepository userRepository;

    public BlogSettingsService(BlogSettingsRepository blogSettingsRepository,
                               UserRepository userRepository) {
        this.blogSettingsRepository = blogSettingsRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public BlogSettingsDto.Response getMySettings(Long userId) {
        getActiveUserOrThrow(userId);
        return blogSettingsRepository.findByUser_Id(userId)
                .map(this::toResponse)
                .orElseGet(this::defaultResponse);
    }

    @Transactional(readOnly = true)
    public BlogSettingsDto.Response getPublicSettings(Long userId) {
        return blogSettingsRepository.findByUser_Id(userId)
                .map(this::toResponse)
                .orElseGet(this::defaultResponse);
    }

    public BlogSettingsDto.Response upsertMySettings(Long userId, BlogSettingsDto.UpdateRequest request) {
        User user = getActiveUserOrThrow(userId);
        BlogSettings settings = blogSettingsRepository.findByUser_Id(userId)
                .orElseGet(() -> BlogSettings.builder().user(user).build());

        if (request.getThemePreset() != null) {
            settings.setThemePreset(parseThemePreset(request.getThemePreset()));
        }
        if (request.getAccentColor() != null) {
            settings.setAccentColor(parseAccentColor(request.getAccentColor()));
        }
        if (request.getCoverImageUrl() != null) {
            settings.setCoverImageUrl(normalizeNullable(request.getCoverImageUrl()));
        }
        if (request.getProfileLayout() != null) {
            settings.setProfileLayout(parseProfileLayout(request.getProfileLayout()));
        }
        if (request.getFontScale() != null) {
            settings.setFontScale(parseFontScale(request.getFontScale()));
        }
        if (request.getShowStats() != null) {
            settings.setShowStats(request.getShowStats());
        }

        BlogSettings saved = blogSettingsRepository.save(settings);
        return toResponse(saved);
    }

    private User getActiveUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> FLAG_NO.equalsIgnoreCase(user.getDeletedYn()))
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private String parseThemePreset(String rawThemePreset) {
        String normalized = normalizeNullable(rawThemePreset);
        if (normalized == null) {
            throw new IllegalArgumentException("themePreset은 빈 값일 수 없습니다.");
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (!ALLOWED_THEME_PRESETS.contains(lowered)) {
            throw new IllegalArgumentException("themePreset은 minimal, ocean, sunset, forest 중 하나여야 합니다.");
        }
        return lowered;
    }

    private String parseProfileLayout(String rawProfileLayout) {
        String normalized = normalizeNullable(rawProfileLayout);
        if (normalized == null) {
            throw new IllegalArgumentException("profileLayout은 빈 값일 수 없습니다.");
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (!ALLOWED_PROFILE_LAYOUTS.contains(lowered)) {
            throw new IllegalArgumentException("profileLayout은 default, compact, centered 중 하나여야 합니다.");
        }
        return lowered;
    }

    private String parseFontScale(String rawFontScale) {
        String normalized = normalizeNullable(rawFontScale);
        if (normalized == null) {
            throw new IllegalArgumentException("fontScale은 빈 값일 수 없습니다.");
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (!ALLOWED_FONT_SCALES.contains(lowered)) {
            throw new IllegalArgumentException("fontScale은 sm, md, lg 중 하나여야 합니다.");
        }
        return lowered;
    }

    private String parseAccentColor(String rawAccentColor) {
        String normalized = normalizeNullable(rawAccentColor);
        if (normalized == null) {
            throw new IllegalArgumentException("accentColor는 빈 값일 수 없습니다.");
        }
        if (!ACCENT_COLOR_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("accentColor는 #RRGGBB 형식이어야 합니다.");
        }
        return normalized;
    }

    private BlogSettingsDto.Response toResponse(BlogSettings settings) {
        return new BlogSettingsDto.Response(
                coerceThemePreset(settings.getThemePreset()),
                coerceAccentColor(settings.getAccentColor()),
                normalizeNullable(settings.getCoverImageUrl()),
                coerceProfileLayout(settings.getProfileLayout()),
                coerceFontScale(settings.getFontScale()),
                settings.getShowStats() == null || settings.getShowStats());
    }

    private BlogSettingsDto.Response defaultResponse() {
        return new BlogSettingsDto.Response(
                BlogSettings.DEFAULT_THEME_PRESET,
                BlogSettings.DEFAULT_ACCENT_COLOR,
                null,
                BlogSettings.DEFAULT_PROFILE_LAYOUT,
                BlogSettings.DEFAULT_FONT_SCALE,
                true);
    }

    private String coerceThemePreset(String rawThemePreset) {
        String normalized = normalizeNullable(rawThemePreset);
        if (normalized == null) {
            return BlogSettings.DEFAULT_THEME_PRESET;
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        return ALLOWED_THEME_PRESETS.contains(lowered) ? lowered : BlogSettings.DEFAULT_THEME_PRESET;
    }

    private String coerceProfileLayout(String rawProfileLayout) {
        String normalized = normalizeNullable(rawProfileLayout);
        if (normalized == null) {
            return BlogSettings.DEFAULT_PROFILE_LAYOUT;
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        return ALLOWED_PROFILE_LAYOUTS.contains(lowered) ? lowered : BlogSettings.DEFAULT_PROFILE_LAYOUT;
    }

    private String coerceFontScale(String rawFontScale) {
        String normalized = normalizeNullable(rawFontScale);
        if (normalized == null) {
            return BlogSettings.DEFAULT_FONT_SCALE;
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        return ALLOWED_FONT_SCALES.contains(lowered) ? lowered : BlogSettings.DEFAULT_FONT_SCALE;
    }

    private String coerceAccentColor(String rawAccentColor) {
        String normalized = normalizeNullable(rawAccentColor);
        if (normalized == null) {
            return BlogSettings.DEFAULT_ACCENT_COLOR;
        }
        if (!ACCENT_COLOR_PATTERN.matcher(normalized).matches()) {
            return BlogSettings.DEFAULT_ACCENT_COLOR;
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
