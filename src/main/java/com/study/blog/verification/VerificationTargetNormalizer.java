package com.study.blog.verification;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class VerificationTargetNormalizer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public String normalizeAndValidate(VerificationChannel channel, String target) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("인증 대상이 비어 있습니다.");
        }
        String trimmed = target.trim();

        return switch (channel) {
            case EMAIL -> normalizeEmail(trimmed);
            case SMS -> normalizePhone(trimmed);
        };
    }

    private String normalizeEmail(String email) {
        String normalized = email.toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("유효한 이메일 형식이 아닙니다.");
        }
        return normalized;
    }

    private String normalizePhone(String phone) {
        String normalized = phone.replaceAll("[^0-9+]", "");
        if (normalized.length() < 8 || normalized.length() > 20) {
            throw new IllegalArgumentException("유효한 휴대폰 번호 형식이 아닙니다.");
        }
        return normalized;
    }
}
