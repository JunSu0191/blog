package com.study.blog.series;

import com.study.blog.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "post_series", indexes = {
        @Index(name = "idx_post_series_owner_id", columnList = "owner_id"),
        @Index(name = "idx_post_series_slug", columnList = "slug")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostSeries {

    private static final int MAX_SLUG_LENGTH = 220;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    void applyDefaults() {
        if (slug == null || slug.isBlank()) {
            slug = slugify(title);
        } else {
            slug = slugify(slug);
        }
        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    private String slugify(String source) {
        if (source == null) {
            return "";
        }
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "");

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9가-힣\\s-]", " ")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        return normalized;
    }
}
