package com.study.blog.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 프로필 JPA 엔티티입니다.
 */
@Entity
@Table(name = "user_profiles", indexes = {
        @Index(name = "idx_user_profiles_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 (1:1 관계)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 표시 이름
    @Column(name = "display_name", length = 50)
    private String displayName;

    // 자기소개
    @Column(columnDefinition = "TEXT")
    private String bio;

    // 아바타 이미지 URL
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    // 웹사이트 URL
    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    // 위치
    @Column(length = 100)
    private String location;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}