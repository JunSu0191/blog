package com.study.blog.oauth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PendingOAuthSignupRepository extends JpaRepository<PendingOAuthSignup, Long> {

    Optional<PendingOAuthSignup> findBySignupToken(String signupToken);

    Optional<PendingOAuthSignup> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    void deleteByExpiresAtBefore(LocalDateTime threshold);
}
