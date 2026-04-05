package com.study.blog.user;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class UserNamePolicy {

    private static final Pattern PUBLIC_USERNAME_PATTERN =
            Pattern.compile("^[a-z0-9](?:[a-z0-9._-]{2,18})[a-z0-9]$");
    private static final Pattern REPEATED_SPECIAL_PATTERN = Pattern.compile(".*([._-])\\1+.*");
    private static final Set<String> RESERVED_USERNAMES = Set.of(
            "admin", "api", "auth", "login", "logout", "signup", "register", "me",
            "root", "system", "settings", "swagger", "actuator", "ws");

    private UserNamePolicy() {
    }

    public static String normalizeUsername(String username) {
        return username == null ? null : username.trim();
    }

    public static String normalizePublicUsername(String username) {
        String normalized = normalizeUsername(username);
        if (normalized == null) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    public static String validatePublicUsername(String username) {
        String normalized = normalizePublicUsername(username);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("아이디를 입력해 주세요.");
        }
        if (!PUBLIC_USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("아이디는 영문 소문자, 숫자, ., _, - 만 사용해 4~20자로 입력해 주세요.");
        }
        if (REPEATED_SPECIAL_PATTERN.matcher(normalized).matches()
                || normalized.contains("..")
                || normalized.contains("__")
                || normalized.contains("--")) {
            throw new IllegalArgumentException("아이디에는 같은 특수문자를 연속해서 사용할 수 없습니다.");
        }
        if (RESERVED_USERNAMES.contains(normalized)) {
            throw new IllegalArgumentException("사용할 수 없는 아이디입니다.");
        }
        return normalized;
    }

    public static String resolveName(String name, String username) {
        String normalizedUsername = normalizeUsername(username);
        if (name != null) {
            String trimmedName = name.trim();
            if (!trimmedName.isEmpty()) {
                return trimmedName;
            }
        }
        return normalizedUsername != null ? normalizedUsername : "";
    }
}
