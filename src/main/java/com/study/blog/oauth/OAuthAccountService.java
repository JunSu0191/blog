package com.study.blog.oauth;

import com.study.blog.user.User;
import com.study.blog.user.UserNamePolicy;
import com.study.blog.user.UserRepository;
import com.study.blog.user.UserRole;
import com.study.blog.user.UserStatus;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OAuthAccountService {

    private static final int MAX_USERNAME_TRIES = 20;
    private static final int USERNAME_MAX_LENGTH = 50;
    private static final Pattern USERNAME_SANITIZE_PATTERN = Pattern.compile("[^a-zA-Z0-9._-]");

    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public OAuthAccountService(OAuthAccountRepository oauthAccountRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder) {
        this.oauthAccountRepository = oauthAccountRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User loginOrRegister(OAuthUserInfo userInfo) {
        OAuthAccount existing = oauthAccountRepository
                .findByProviderAndProviderUserId(userInfo.provider(), userInfo.providerUserId())
                .orElse(null);
        if (existing != null) {
            User user = validateLoginUser(existing.getUser());
            existing.setLastLoginAt(LocalDateTime.now());
            oauthAccountRepository.save(existing);
            return user;
        }

        User createdUser = createUserWithUniqueUsername(userInfo);
        OAuthAccount account = OAuthAccount.builder()
                .user(createdUser)
                .provider(userInfo.provider())
                .providerUserId(userInfo.providerUserId())
                .lastLoginAt(LocalDateTime.now())
                .build();

        try {
            oauthAccountRepository.saveAndFlush(account);
            return createdUser;
        } catch (DataIntegrityViolationException ex) {
            OAuthAccount concurrent = oauthAccountRepository
                    .findByProviderAndProviderUserId(userInfo.provider(), userInfo.providerUserId())
                    .orElse(null);
            if (concurrent != null) {
                rollbackCreatedUser(createdUser);
                User user = validateLoginUser(concurrent.getUser());
                concurrent.setLastLoginAt(LocalDateTime.now());
                oauthAccountRepository.save(concurrent);
                return user;
            }

            throw new OAuth2LoginException(
                    "oauth_account_conflict",
                    "소셜 로그인 처리 중 충돌이 발생했습니다. 다시 시도해 주세요.",
                    ex);
        }
    }

    private User createUserWithUniqueUsername(OAuthUserInfo userInfo) {
        String baseUsername = buildBaseUsername(userInfo);
        OAuth2LoginException lastError = null;

        for (int attempt = 0; attempt < MAX_USERNAME_TRIES; attempt++) {
            String candidate = applyUsernameSuffix(baseUsername, attempt);
            if (userRepository.existsByUsername(candidate) || userRepository.existsByNickname(candidate)) {
                continue;
            }

            User user = User.builder()
                    .username(candidate)
                    .nickname(candidate)
                    .email(resolveAvailableEmail(userInfo.email()))
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .name(resolveDisplayName(userInfo, candidate))
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .mustChangePassword(false)
                    .deletedYn("N")
                    .createdAt(LocalDateTime.now())
                    .build();
            try {
                return userRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException ex) {
                lastError = new OAuth2LoginException(
                        "username_conflict",
                        "소셜 회원가입 중 사용자 ID 생성에 실패했습니다.",
                        ex);
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new OAuth2LoginException(
                "username_generation_failed",
                "소셜 회원가입 중 사용자 ID를 생성할 수 없습니다.");
    }

    private User validateLoginUser(User user) {
        if (user == null) {
            throw new OAuth2LoginException("oauth_user_not_found", "소셜 계정에 연결된 사용자를 찾을 수 없습니다.");
        }
        if (!"N".equalsIgnoreCase(user.getDeletedYn())) {
            throw new OAuth2LoginException("account_deleted", "삭제된 계정입니다.");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new OAuth2LoginException("account_suspended", "정지된 계정입니다.");
        }
        return user;
    }

    private String resolveDisplayName(OAuthUserInfo userInfo, String fallbackUsername) {
        String name = normalize(userInfo.name());
        if (name != null) {
            return name;
        }
        String email = normalize(userInfo.email());
        if (email != null && email.contains("@")) {
            String localPart = normalize(email.substring(0, email.indexOf("@")));
            if (localPart != null) {
                return localPart;
            }
        }
        return fallbackUsername;
    }

    private String resolveAvailableEmail(String email) {
        String normalized = normalize(email);
        if (normalized == null) {
            return null;
        }
        return userRepository.existsByEmail(normalized) ? null : normalized;
    }

    private String buildBaseUsername(OAuthUserInfo userInfo) {
        String email = normalize(userInfo.email());
        if (email != null && email.contains("@")) {
            String localPart = normalize(email.substring(0, email.indexOf("@")));
            if (localPart != null) {
                return truncate(sanitize(localPart));
            }
        }

        String raw = userInfo.provider().name().toLowerCase(Locale.ROOT) + "_" + userInfo.providerUserId();
        String sanitized = truncate(sanitize(raw));
        if (sanitized.isBlank()) {
            return "user";
        }
        return sanitized;
    }

    private String applyUsernameSuffix(String baseUsername, int attempt) {
        String normalizedBase = UserNamePolicy.normalizeUsername(baseUsername);
        if (normalizedBase == null || normalizedBase.isBlank()) {
            normalizedBase = "user";
        }
        if (attempt == 0) {
            return truncate(normalizedBase);
        }
        String suffix = "_" + attempt;
        String truncatedBase = truncate(normalizedBase, USERNAME_MAX_LENGTH - suffix.length());
        return truncatedBase + suffix;
    }

    private String sanitize(String source) {
        String replaced = USERNAME_SANITIZE_PATTERN.matcher(source).replaceAll("_");
        String trimmed = replaced.replaceAll("_+", "_");
        return normalize(trimmed) == null ? "" : trimmed;
    }

    private String truncate(String value) {
        return truncate(value, USERNAME_MAX_LENGTH);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void rollbackCreatedUser(User createdUser) {
        try {
            userRepository.delete(createdUser);
            userRepository.flush();
        } catch (Exception cleanupEx) {
            log.warn("oauth race cleanup failed. userId={}, username={}",
                    createdUser.getId(), createdUser.getUsername(), cleanupEx);
        }
    }
}
