package com.study.blog.verification;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class VerificationService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final VerificationTargetNormalizer targetNormalizer;
    private final VerificationMessageSender messageSender;
    private final VerificationRateLimiter rateLimiter;
    private final long codeExpireSeconds;
    private final int maxAttempts;
    private final long resendCooldownSeconds;
    private final String hashSecret;
    private final boolean mockExposeCode;

    public VerificationService(VerificationCodeRepository verificationCodeRepository,
                               VerificationTargetNormalizer targetNormalizer,
                               VerificationMessageSender messageSender,
                               VerificationRateLimiter rateLimiter,
                               @Value("${app.verification.code-expire-seconds:300}") long codeExpireSeconds,
                               @Value("${app.verification.max-attempts:5}") int maxAttempts,
                               @Value("${app.verification.resend-cooldown-seconds:60}") long resendCooldownSeconds,
                               @Value("${app.verification.hash-secret:change-this-verification-hash-secret}") String hashSecret,
                               @Value("${app.verification.mock-expose-code:false}") boolean mockExposeCode) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.targetNormalizer = targetNormalizer;
        this.messageSender = messageSender;
        this.rateLimiter = rateLimiter;
        this.codeExpireSeconds = codeExpireSeconds;
        this.maxAttempts = maxAttempts;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.hashSecret = hashSecret;
        this.mockExposeCode = mockExposeCode;
    }

    public SendResult send(VerificationPurpose purpose,
                           VerificationChannel channel,
                           String rawTarget,
                           String requesterIp,
                           boolean dispatchDelivery) {
        String normalizedTarget = targetNormalizer.normalizeAndValidate(channel, rawTarget);
        String ipKey = requesterIp == null ? "" : requesterIp.trim();

        rateLimiter.checkAndCount("verification-target:" + normalizedTarget + ":" + purpose.name());
        rateLimiter.checkAndCount("verification-ip:" + ipKey);

        LocalDateTime now = LocalDateTime.now();
        VerificationCode latest = verificationCodeRepository
                .findTopByTargetAndPurposeOrderByCreatedAtDesc(normalizedTarget, purpose)
                .orElse(null);

        if (latest != null && latest.getCreatedAt() != null
                && latest.getCreatedAt().plusSeconds(resendCooldownSeconds).isAfter(now)) {
            throw new IllegalStateException("인증번호 재요청은 잠시 후 다시 시도해 주세요.");
        }

        String code = generateCode();
        LocalDateTime expiresAt = now.plusSeconds(codeExpireSeconds);
        int resendCount = latest == null ? 0 : latest.getResendCount() + 1;

        VerificationCode verificationCode = VerificationCode.builder()
                .purpose(purpose)
                .channel(channel)
                .target(normalizedTarget)
                .codeHash(hashCode(purpose, channel, normalizedTarget, code))
                .expiresAt(expiresAt)
                .attempts(0)
                .resendCount(resendCount)
                .build();

        verificationCodeRepository.save(verificationCode);

        if (dispatchDelivery) {
            messageSender.send(channel, normalizedTarget, buildMessage(code, purpose));
        }

        String debugCode = mockExposeCode ? code : null;
        return new SendResult(verificationCode, debugCode, resendCooldownSeconds);
    }

    public VerificationCode confirm(Long verificationId,
                                    String rawCode,
                                    VerificationPurpose expectedPurpose) {
        VerificationCode verificationCode = verificationCodeRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("인증 정보를 찾을 수 없습니다."));
        if (expectedPurpose != null && verificationCode.getPurpose() != expectedPurpose) {
            throw new IllegalArgumentException("인증 목적이 일치하지 않습니다.");
        }

        if (verificationCode.getVerifiedAt() != null) {
            return verificationCode;
        }

        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("인증 코드가 만료되었습니다.");
        }

        int attempts = verificationCode.getAttempts() == null ? 0 : verificationCode.getAttempts();
        if (attempts >= maxAttempts) {
            throw new IllegalStateException("인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해 주세요.");
        }

        String code = normalizeCode(rawCode);
        String expectedHash = hashCode(
                verificationCode.getPurpose(),
                verificationCode.getChannel(),
                verificationCode.getTarget(),
                code);

        if (!expectedHash.equals(verificationCode.getCodeHash())) {
            verificationCode.setAttempts(attempts + 1);
            verificationCodeRepository.save(verificationCode);
            if (verificationCode.getAttempts() >= maxAttempts) {
                throw new IllegalStateException("인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해 주세요.");
            }
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }

        verificationCode.setVerifiedAt(LocalDateTime.now());
        verificationCodeRepository.save(verificationCode);
        return verificationCode;
    }

    public VerificationCode requireVerified(Long verificationId,
                                            VerificationPurpose purpose,
                                            VerificationChannel channel,
                                            String rawTarget) {
        VerificationCode verificationCode = verificationCodeRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("인증 정보를 찾을 수 없습니다."));

        if (verificationCode.getPurpose() != purpose) {
            throw new IllegalArgumentException("인증 목적이 일치하지 않습니다.");
        }
        if (verificationCode.getChannel() != channel) {
            throw new IllegalArgumentException("인증 채널이 일치하지 않습니다.");
        }
        String normalizedTarget = targetNormalizer.normalizeAndValidate(channel, rawTarget);
        if (!normalizedTarget.equals(verificationCode.getTarget())) {
            throw new IllegalArgumentException("인증 대상이 일치하지 않습니다.");
        }
        if (verificationCode.getVerifiedAt() == null) {
            throw new IllegalStateException("휴대폰 인증이 완료되지 않았습니다.");
        }
        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("인증 코드가 만료되었습니다.");
        }
        return verificationCode;
    }

    public String normalizeTarget(VerificationChannel channel, String target) {
        return targetNormalizer.normalizeAndValidate(channel, target);
    }

    private String buildMessage(String code, VerificationPurpose purpose) {
        return switch (purpose) {
            case SIGNUP -> "회원가입 인증번호: " + code;
            case FIND_ID -> "아이디 찾기 인증번호: " + code;
            case RESET_PASSWORD -> "비밀번호 재설정 인증번호: " + code;
        };
    }

    private String generateCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
    }

    private String normalizeCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("인증번호가 비어 있습니다.");
        }
        String trimmed = code.trim();
        if (!trimmed.matches("\\d{6}")) {
            throw new IllegalArgumentException("인증번호 형식이 올바르지 않습니다.");
        }
        return trimmed;
    }

    private String hashCode(VerificationPurpose purpose, VerificationChannel channel, String target, String code) {
        String payload = purpose.name() + "|" + channel.name() + "|" + target + "|" + code + "|" + hashSecret;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("해시 알고리즘을 초기화할 수 없습니다.", ex);
        }
    }

    public record SendResult(VerificationCode verificationCode, String debugCode, long cooldownSeconds) {
    }
}
