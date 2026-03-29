package com.study.blog.bookmark;

import com.study.blog.post.Post;
import com.study.blog.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_bookmarks", indexes = {
        @Index(name = "idx_post_bookmarks_user_id", columnList = "user_id"),
        @Index(name = "idx_post_bookmarks_deleted_yn", columnList = "deleted_yn"),
        @Index(name = "idx_post_bookmarks_bookmarked_at", columnList = "bookmarked_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bookmarked_at", nullable = false)
    @Builder.Default
    private LocalDateTime bookmarkedAt = LocalDateTime.now();

    @Column(name = "deleted_yn", nullable = false, columnDefinition = "CHAR(1)")
    @Builder.Default
    private String deletedYn = "N";

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    void applyDefaults() {
        if (deletedYn == null || deletedYn.isBlank()) {
            deletedYn = "N";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (bookmarkedAt == null) {
            bookmarkedAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }
}
