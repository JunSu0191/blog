package com.study.blog.oauth;

import com.study.blog.user.User;
import com.study.blog.user.UserNamePolicy;
import com.study.blog.user.UserRepository;
import com.study.blog.user.UserRole;
import com.study.blog.user.UserStatus;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final PendingOAuthSignupRepository pendingOAuthSignupRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final long pendingSignupExpireSeconds;

    public OAuthAccountService(OAuthAccountRepository oauthAccountRepository,
                               PendingOAuthSignupRepository pendingOAuthSignupRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.oauth2.pending-signup-expire-seconds:1800}") long pendingSignupExpireSeconds) {
        this.oauthAccountRepository = oauthAccountRepository;
        this.pendingOAuthSignupRepository = pendingOAuthSignupRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.pendingSignupExpireSeconds = pendingSignupExpireSeconds;
    }

    @Transactional
    public OAuthLoginResult loginOrPrepareSignup(OAuthUserInfo userInfo) {
        OAuthAccount existing = oauthAccountRepository
                .findByProviderAndProviderUserId(userInfo.provider(), userInfo.providerUserId())
                .orElse(null);
        if (existing != null) {
            User user = validateLoginUser(existing.getUser());
            existing.setLastLoginAt(LocalDateTime.now());
            oauthAccountRepository.save(existing);
            return OAuthLoginResult.completed(user);
        }

        pendingOAuthSignupRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        PendingOAuthSignup pendingSignup = pendingOAuthSignupRepository
                .findByProviderAndProviderUserId(userInfo.provider(), userInfo.providerUserId())
                .orElseGet(PendingOAuthSignup::new);

        pendingSignup.setProvider(userInfo.provider());
        pendingSignup.setProviderUserId(userInfo.providerUserId());
        pendingSignup.setEmail(normalize(userInfo.email()));
        pendingSignup.setName(normalize(userInfo.name()));
        pendingSignup.setSignupToken(UUID.randomUUID().toString());
        pendingSignup.setExpiresAt(LocalDateTime.now().plusSeconds(pendingSignupExpireSeconds));

        PendingOAuthSignup saved = pendingOAuthSignupRepository.saveAndFlush(pendingSignup);
        return OAuthLoginResult.pending(saved.getSignupToken());
    }

    @Transactional
    public PendingSignupProfile getPendingSignupProfile(String signupToken) {
        PendingOAuthSignup pendingSignup = requireValidPendingSignup(signupToken);
        String suggestedUsername = suggestUsername(toUserInfo(pendingSignup));
        String suggestedNickname = resolveDisplayName(toUserInfo(pendingSignup), suggestedUsername);
        return new PendingSignupProfile(
                pendingSignup.getSignupToken(),
                pendingSignup.getProvider().name(),
                pendingSignup.getEmail(),
                pendingSignup.getName(),
                suggestedUsername,
                suggestedNickname);
    }

    @Transactional
    public User completeSignup(String signupToken, String username, String nickname) {
        PendingOAuthSignup pendingSignup = requireValidPendingSignup(signupToken);
        String normalizedUsername = UserNamePolicy.validatePublicUsername(username);
        String normalizedNickname = normalize(nickname);
        if (normalizedNickname == null) {
            throw new IllegalArgumentException("닉네임을 입력해 주세요.");
        }

        OAuthAccount existing = oauthAccountRepository
                .findByProviderAndProviderUserId(pendingSignup.getProvider(), pendingSignup.getProviderUserId())
                .orElse(null);
        if (existing != null) {
            pendingOAuthSignupRepository.delete(pendingSignup);
            return validateLoginUser(existing.getUser());
        }

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalStateException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByNickname(normalizedNickname)) {
            throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
        }

        User createdUser = User.builder()
                .username(normalizedUsername)
                .nickname(normalizedNickname)
                .email(resolveAvailableEmail(pendingSignup.getEmail()))
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .name(resolveDisplayName(toUserInfo(pendingSignup), normalizedNickname))
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .mustChangePassword(false)
                .deletedYn("N")
                .createdAt(LocalDateTime.now())
                .build();

        try {
            createdUser = userRepository.saveAndFlush(createdUser);
            OAuthAccount account = OAuthAccount.builder()
                    .user(createdUser)
                    .provider(pendingSignup.getProvider())
                    .providerUserId(pendingSignup.getProviderUserId())
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            oauthAccountRepository.saveAndFlush(account);
            pendingOAuthSignupRepository.delete(pendingSignup);
            return createdUser;
        } catch (DataIntegrityViolationException ex) {
            OAuthAccount concurrent = oauthAccountRepository
                    .findByProviderAndProviderUserId(pendingSignup.getProvider(), pendingSignup.getProviderUserId())
                    .orElse(null);
            if (concurrent != null) {
                rollbackCreatedUser(createdUser);
                pendingOAuthSignupRepository.delete(pendingSignup);
                User user = validateLoginUser(concurrent.getUser());
                concurrent.setLastLoginAt(LocalDateTime.now());
                oauthAccountRepository.save(concurrent);
                return user;
            }

            rollbackCreatedUser(createdUser);
            throw new IllegalStateException("소셜 회원가입 완료 처리 중 충돌이 발생했습니다. 다시 시도해 주세요.");
        }
    }

    private PendingOAuthSignup requireValidPendingSignup(String signupToken) {
        String normalizedToken = normalize(signupToken);
        if (normalizedToken == null) {
            throw new IllegalArgumentException("회원가입 토큰이 없습니다.");
        }
        PendingOAuthSignup pendingSignup = pendingOAuthSignupRepository.findBySignupToken(normalizedToken)
                .orElseThrow(() -> new IllegalArgumentException("소셜 회원가입 정보를 찾을 수 없습니다."));
        if (pendingSignup.getExpiresAt() != null && pendingSignup.getExpiresAt().isBefore(LocalDateTime.now())) {
            pendingOAuthSignupRepository.delete(pendingSignup);
            throw new IllegalArgumentException("소셜 회원가입 정보가 만료되었습니다. 다시 로그인해 주세요.");
        }
        return pendingSignup;
    }

    private String suggestUsername(OAuthUserInfo userInfo) {
        String baseUsername = buildBaseUsername(userInfo);

        for (int attempt = 0; attempt < MAX_USERNAME_TRIES; attempt++) {
            String candidate = applyUsernameSuffix(baseUsername, attempt);
            String normalizedCandidate;
            try {
                normalizedCandidate = UserNamePolicy.validatePublicUsername(candidate);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            if (!userRepository.existsByUsername(normalizedCandidate)) {
                return normalizedCandidate;
            }
        }
        return "user" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
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
                return truncate(sanitize(localPart.toLowerCase(Locale.ROOT)));
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
        String normalizedBase = UserNamePolicy.normalizePublicUsername(baseUsername);
        if (normalizedBase == null || normalizedBase.isBlank()) {
            normalizedBase = "user";
        }
        if (attempt == 0) {
            return truncate(normalizedBase);
        }
        String suffix = "-" + attempt;
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

    private OAuthUserInfo toUserInfo(PendingOAuthSignup pendingSignup) {
        return new OAuthUserInfo(
                pendingSignup.getProvider(),
                pendingSignup.getProviderUserId(),
                pendingSignup.getEmail(),
                pendingSignup.getName());
    }

    private void rollbackCreatedUser(User createdUser) {
        if (createdUser == null || createdUser.getId() == null) {
            return;
        }
        try {
            userRepository.delete(createdUser);
            userRepository.flush();
        } catch (Exception cleanupEx) {
            log.warn("oauth race cleanup failed. userId={}, username={}",
                    createdUser.getId(), createdUser.getUsername(), cleanupEx);
        }
    }

    public record OAuthLoginResult(User user, boolean needsProfileSetup, String signupToken) {

        public static OAuthLoginResult completed(User user) {
            return new OAuthLoginResult(user, false, null);
        }

        public static OAuthLoginResult pending(String signupToken) {
            return new OAuthLoginResult(null, true, signupToken);
        }
    }

    public record PendingSignupProfile(String signupToken, String provider,
                                       String email, String name,
                                       String suggestedUsername, String suggestedNickname) {
    }
}
