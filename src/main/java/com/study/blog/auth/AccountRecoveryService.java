package com.study.blog.auth;

import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import com.study.blog.user.UserStatus;
import com.study.blog.verification.VerificationChannel;
import com.study.blog.verification.VerificationCode;
import com.study.blog.verification.VerificationPurpose;
import com.study.blog.verification.VerificationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Transactional
public class AccountRecoveryService {

    private final VerificationService verificationService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final long resetTokenExpireSeconds;
    private final String tokenHashSecret;

    public AccountRecoveryService(VerificationService verificationService,
                                  UserRepository userRepository,
                                  PasswordResetTokenRepository passwordResetTokenRepository,
                                  PasswordEncoder passwordEncoder,
                                  @Value("${app.auth.reset-token-expire-seconds:900}") long resetTokenExpireSeconds,
                                  @Value("${app.auth.reset-token-hash-secret:change-this-reset-token-secret}") String tokenHashSecret) {
        this.verificationService = verificationService;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.resetTokenExpireSeconds = resetTokenExpireSeconds;
        this.tokenHashSecret = tokenHashSecret;
    }

    public VerificationService.SendResult requestFindId(VerificationChannel channel, String target, String requesterIp) {
        String normalizedTarget = verificationService.normalizeTarget(channel, target);
        boolean userExists = findActiveUser(channel, normalizedTarget) != null;
        return verificationService.send(VerificationPurpose.FIND_ID, channel, normalizedTarget, requesterIp, userExists);
    }

    public String confirmFindId(Long verificationId, String code) {
        VerificationCode verificationCode = verificationService.confirm(verificationId, code, VerificationPurpose.FIND_ID);
        User user = findActiveUser(verificationCode.getChannel(), verificationCode.getTarget());
        if (user == null) {
            throw new IllegalArgumentException("일치하는 계정을 찾을 수 없습니다.");
        }
        return maskUsername(user.getUsername());
    }

    public VerificationService.SendResult requestResetPassword(VerificationChannel channel, String target, String requesterIp) {
        String normalizedTarget = verificationService.normalizeTarget(channel, target);
        boolean userExists = findActiveUser(channel, normalizedTarget) != null;
        return verificationService.send(VerificationPurpose.RESET_PASSWORD, channel, normalizedTarget, requesterIp, userExists);
    }

    public ResetTokenResult confirmResetPassword(Long verificationId, String code) {
        VerificationCode verificationCode = verificationService.confirm(verificationId, code, VerificationPurpose.RESET_PASSWORD);
        User user = findActiveUser(verificationCode.getChannel(), verificationCode.getTarget());
        if (user == null) {
            throw new IllegalArgumentException("비밀번호를 재설정할 계정을 찾을 수 없습니다.");
        }

        String resetToken = generateResetToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(resetTokenExpireSeconds);
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(hashResetToken(resetToken))
                .expiresAt(expiresAt)
                .usedAt(null)
                .build());
        return new ResetTokenResult(resetToken, expiresAt);
    }

    public void resetPassword(String resetToken, String newPassword) {
        String normalizedToken = normalizeToken(resetToken);
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hashResetToken(normalizedToken))
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 비밀번호 재설정 토큰입니다."));
        if (token.getUsedAt() != null) {
            throw new IllegalStateException("이미 사용된 비밀번호 재설정 토큰입니다.");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("만료된 비밀번호 재설정 토큰입니다.");
        }

        User user = token.getUser();
        if (user == null || !"N".equalsIgnoreCase(user.getDeletedYn()) || user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("비밀번호를 변경할 수 없는 계정입니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);

        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);
        userRepository.save(user);
    }

    private User findActiveUser(VerificationChannel channel, String normalizedTarget) {
        return switch (channel) {
            case EMAIL -> userRepository.findByEmailAndDeletedYn(normalizedTarget, "N")
                    .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                    .orElse(null);
            case SMS -> userRepository.findByPhoneNumberAndDeletedYn(normalizedTarget, "N")
                    .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                    .orElse(null);
        };
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("재설정 토큰이 비어 있습니다.");
        }
        return token.trim();
    }

    private String generateResetToken() {
        byte[] randomBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashResetToken(String token) {
        String payload = token + "|" + tokenHashSecret;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("해시 알고리즘을 초기화할 수 없습니다.", ex);
        }
    }

    private String maskUsername(String username) {
        if (username == null || username.isBlank()) {
            return "***";
        }
        if (username.length() <= 2) {
            return username.charAt(0) + "*";
        }
        int keepPrefix = Math.min(2, username.length() - 1);
        StringBuilder builder = new StringBuilder();
        builder.append(username, 0, keepPrefix);
        for (int i = keepPrefix; i < username.length() - 1; i++) {
            builder.append('*');
        }
        builder.append(username.charAt(username.length() - 1));
        return builder.toString();
    }

    public record ResetTokenResult(String resetToken, LocalDateTime expiresAt) {
    }
}
