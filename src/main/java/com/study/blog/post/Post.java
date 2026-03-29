package com.study.blog.post;

import com.study.blog.attach.AttachFile;
import com.study.blog.category.Category;
import com.study.blog.series.PostSeries;
import com.study.blog.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 제공된 SQL 스키마에 맞춘 Post JPA 엔티티입니다.
 *
 * 예시 SQL:
 * CREATE TABLE posts (
 *   id BIGINT AUTO_INCREMENT PRIMARY KEY,
 *   user_id BIGINT NOT NULL,
 *   title VARCHAR(200) NOT NULL,
 *   content TEXT,
 *   deleted_yn CHAR(1) DEFAULT 'N',
 *   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *   FOREIGN KEY (user_id) REFERENCES users(id)
 * );
 */
@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_post_user_id", columnList = "user_id"),
        @Index(name = "idx_post_created_at", columnList = "created_at"),
        @Index(name = "idx_posts_category_id", columnList = "category_id"),
        @Index(name = "idx_posts_series_id", columnList = "series_id"),
        @Index(name = "idx_posts_scheduled_at", columnList = "scheduled_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User와의 다대일 관계입니다. 외래키는 `user_id` 컬럼에 저장됩니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 카테고리
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 255)
    private String subtitle;

    @Column(nullable = false, length = 220, unique = true)
    private String slug;

    @Column(name = "meta_title", length = 255)
    private String metaTitle;

    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    @Column(length = 500)
    private String excerpt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Lob
    @Column(name = "content_json", columnDefinition = "LONGTEXT")
    private String contentJson;

    @Lob
    @Column(name = "content_html", columnDefinition = "LONGTEXT")
    private String contentHtml;

    @Lob
    @Column(name = "toc_json", columnDefinition = "LONGTEXT")
    private String tocJson;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PostStatus status = PostStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    private PostSeries series;

    @Column(name = "series_order")
    private Integer seriesOrder;

    @Column(name = "read_time_minutes", nullable = false)
    @Builder.Default
    private Integer readTimeMinutes = 0;

    @Column(name = "deleted_yn", columnDefinition = "CHAR(1)")
    @Builder.Default
    private String deletedYn = "N";

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // 조회수 (캐시용)
    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    // 좋아요 수 (캐시용)
    @Column(name = "like_count")
    @Builder.Default
    private Long likeCount = 0L;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY)
    private List<AttachFile> attachFiles = new ArrayList<>();

    public boolean isDeleted() {
        return deletedAt != null || "Y".equalsIgnoreCase(deletedYn);
    }

    public boolean isPublished() {
        return status == PostStatus.PUBLISHED && !isDeleted();
    }

    public boolean isPubliclyListed() {
        return isPublished() && visibility == PostVisibility.PUBLIC;
    }
}
