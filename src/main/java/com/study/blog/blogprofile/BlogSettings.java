package com.study.blog.blogprofile;

import com.study.blog.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "blog_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogSettings {

    public static final String DEFAULT_THEME_PRESET = "minimal";
    public static final String DEFAULT_ACCENT_COLOR = "#2563eb";
    public static final String DEFAULT_PROFILE_LAYOUT = "default";
    public static final String DEFAULT_FONT_SCALE = "md";

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "theme_preset", nullable = false, length = 20)
    @Builder.Default
    private String themePreset = DEFAULT_THEME_PRESET;

    @Column(name = "accent_color", nullable = false, length = 7)
    @Builder.Default
    private String accentColor = DEFAULT_ACCENT_COLOR;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Column(name = "profile_layout", nullable = false, length = 20)
    @Builder.Default
    private String profileLayout = DEFAULT_PROFILE_LAYOUT;

    @Column(name = "font_scale", nullable = false, length = 10)
    @Builder.Default
    private String fontScale = DEFAULT_FONT_SCALE;

    @Column(name = "show_stats", nullable = false)
    @Builder.Default
    private Boolean showStats = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (themePreset == null || themePreset.isBlank()) {
            themePreset = DEFAULT_THEME_PRESET;
        }
        if (accentColor == null || accentColor.isBlank()) {
            accentColor = DEFAULT_ACCENT_COLOR;
        }
        if (profileLayout == null || profileLayout.isBlank()) {
            profileLayout = DEFAULT_PROFILE_LAYOUT;
        }
        if (fontScale == null || fontScale.isBlank()) {
            fontScale = DEFAULT_FONT_SCALE;
        }
        if (showStats == null) {
            showStats = true;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (themePreset == null || themePreset.isBlank()) {
            themePreset = DEFAULT_THEME_PRESET;
        }
        if (accentColor == null || accentColor.isBlank()) {
            accentColor = DEFAULT_ACCENT_COLOR;
        }
        if (profileLayout == null || profileLayout.isBlank()) {
            profileLayout = DEFAULT_PROFILE_LAYOUT;
        }
        if (fontScale == null || fontScale.isBlank()) {
            fontScale = DEFAULT_FONT_SCALE;
        }
        if (showStats == null) {
            showStats = true;
        }
    }
}
