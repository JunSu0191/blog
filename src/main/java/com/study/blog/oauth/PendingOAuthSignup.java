package com.study.blog.oauth;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "pending_oauth_signups",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pending_oauth_signups_token", columnNames = "signup_token"),
                @UniqueConstraint(name = "uk_pending_oauth_signups_provider_user",
                        columnNames = {"provider", "provider_user_id"})
        },
        indexes = {
                @Index(name = "idx_pending_oauth_signups_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingOAuthSignup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 191)
    private String providerUserId;

    @Column(length = 255)
    private String email;

    @Column(length = 100)
    private String name;

    @Column(name = "signup_token", nullable = false, length = 128)
    private String signupToken;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touchTimestamps() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }
}
