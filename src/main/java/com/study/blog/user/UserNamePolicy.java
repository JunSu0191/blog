package com.study.blog.user;

public final class UserNamePolicy {

    private UserNamePolicy() {
    }

    public static String normalizeUsername(String username) {
        return username == null ? null : username.trim();
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
