package com.study.blog.tag;

import com.study.blog.post.Post;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 게시글과 태그의 다대다 관계를 위한 연결 엔티티입니다.
 */
@Entity
@Table(name = "post_tags", indexes = {
        @Index(name = "idx_post_tags_post_id", columnList = "post_id"),
        @Index(name = "idx_post_tags_tag_id", columnList = "tag_id"),
        @Index(name = "idx_post_tags_deleted_yn", columnList = "deleted_yn")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(name = "deleted_yn", columnDefinition = "CHAR(1)")
    private String deletedYn;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}