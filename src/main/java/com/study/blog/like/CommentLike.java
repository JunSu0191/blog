package com.study.blog.like;

import com.study.blog.comment.Comment;
import com.study.blog.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 댓글 좋아요 JPA 엔티티입니다.
 */
@Entity
@Table(name = "comment_likes", indexes = {
        @Index(name = "idx_comment_likes_comment_id", columnList = "comment_id"),
        @Index(name = "idx_comment_likes_user_id", columnList = "user_id"),
        @Index(name = "idx_comment_likes_deleted_yn", columnList = "deleted_yn")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "deleted_yn", columnDefinition = "CHAR(1)")
    private String deletedYn;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}