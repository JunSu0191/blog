package com.study.blog.like;

import com.study.blog.post.Post;
import com.study.blog.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 게시글 좋아요 JPA 엔티티입니다.
 */
@Entity
@Table(name = "post_likes", indexes = {
        @Index(name = "idx_post_likes_post_id", columnList = "post_id"),
        @Index(name = "idx_post_likes_user_id", columnList = "user_id"),
        @Index(name = "idx_post_likes_deleted_yn", columnList = "deleted_yn")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "deleted_yn", columnDefinition = "CHAR(1)")
    private String deletedYn;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}