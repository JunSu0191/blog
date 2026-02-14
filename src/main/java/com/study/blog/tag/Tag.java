package com.study.blog.tag;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 태그 JPA 엔티티입니다.
 */
@Entity
@Table(name = "tags", indexes = {
        @Index(name = "idx_tags_name", columnList = "name"),
        @Index(name = "idx_tags_deleted_yn", columnList = "deleted_yn")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "deleted_yn", columnDefinition = "CHAR(1)")
    private String deletedYn = "N";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}