package com.study.blog.post;

import com.study.blog.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_images", indexes = {
        @Index(name = "idx_post_images_uploader_id", columnList = "uploader_id"),
        @Index(name = "idx_post_images_post_id", columnList = "post_id"),
        @Index(name = "idx_post_images_draft_id", columnList = "draft_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id")
    private PostDraft draft;

    @Column(nullable = false, length = 500)
    private String url;

    private Integer width;

    private Integer height;

    private Long size;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

