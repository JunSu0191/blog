package com.study.blog.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false)
    private String password;

    @Column(length = 255)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "deleted_yn", columnDefinition = "CHAR(1)")
    @Builder.Default
    private String deletedYn = "N";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = false;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public String getName() {
        return UserNamePolicy.resolveName(this.name, this.username);
    }

    @PrePersist
    @PreUpdate
    private void applyNamePolicy() {
        this.username = UserNamePolicy.normalizeUsername(this.username);
        this.name = UserNamePolicy.resolveName(this.name, this.username);
        this.nickname = normalizeNullable(this.nickname);
        if (this.nickname == null) {
            this.nickname = this.username;
        }
        this.email = normalizeNullable(this.email);
        this.phoneNumber = normalizeNullable(this.phoneNumber);
        if (this.deletedYn == null || this.deletedYn.isBlank()) {
            this.deletedYn = "N";
        }
        if (this.role == null) {
            this.role = UserRole.USER;
        }
        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }
        if (this.mustChangePassword == null) {
            this.mustChangePassword = false;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
