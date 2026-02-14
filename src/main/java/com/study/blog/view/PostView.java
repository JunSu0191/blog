package com.study.blog.view;

import com.study.blog.post.Post;
import com.study.blog.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 게시글 조회수 JPA 엔티티입니다.
 */
@Entity
@Table(name = "post_views", indexes = {
        @Index(name = "idx_post_views_post_id", columnList = "post_id"),
        @Index(name = "idx_post_views_user_id", columnList = "user_id"),
        @Index(name = "idx_post_views_viewed_at", columnList = "viewed_at"),
        @Index(name = "idx_post_views_deleted_yn", columnList = "deleted_yn")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "deleted_yn", columnDefinition = "CHAR(1)")
    private String deletedYn;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}