package com.study.blog.core.security;

import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import com.study.blog.user.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
/**
 * 현재 요청의 "실제 사용자 ID"를 결정하는 유틸리티.
 *
 * 우선순위:
 * 1) Spring SecurityContext (JWT 인증 성공 시)
 * 2) 개발용 X-User-Id 헤더
 * 3) application.yml의 app.auth.dev-user-id
 * 4) 활성 사용자 중 가장 작은 ID (로컬 개발 편의)
 */
public class CurrentUserResolver {

    private final UserRepository userRepository;
    private final boolean allowDevFallback;
    private final Long devUserId;

    public CurrentUserResolver(UserRepository userRepository,
                               @Value("${app.auth.allow-dev-fallback:false}") boolean allowDevFallback,
                               @Value("${app.auth.dev-user-id:}") String devUserId) {
        this.userRepository = userRepository;
        this.allowDevFallback = allowDevFallback;
        if (devUserId == null || devUserId.isBlank()) {
            this.devUserId = null;
        } else {
            this.devUserId = Long.parseLong(devUserId);
        }
    }

    public Long resolveFromRest(Long xUserId) {
        // 1) JWT 인증 정보(SecurityContext)가 있으면 최우선 사용
        Long securityUserId = resolveFromAuthentication(SecurityContextHolder.getContext().getAuthentication());
        if (securityUserId != null) {
            return securityUserId;
        }

        // 2) REST 호출에서 X-User-Id 헤더를 받은 경우(개발용 fallback)
        if (xUserId != null) {
            User activeUser = getActiveUserById(xUserId);
            if (activeUser != null) {
                return activeUser.getId();
            }
        }

        // 3~4) 개발환경 fallback (설정된 ID -> 첫 활성 유저)
        Long fallbackUserId = resolveDevFallbackUserId();
        if (fallbackUserId != null) {
            return fallbackUserId;
        }

        throw new AuthenticationCredentialsNotFoundException("인증 사용자 정보를 확인할 수 없습니다.");
    }

    public Long resolveFromWebSocket(Principal principal, Long xUserId) {
        // 1) STOMP Principal로 식별 시도
        Long principalUserId = resolveFromPrincipal(principal);
        if (principalUserId != null) {
            return principalUserId;
        }

        // 2) WebSocket native header X-User-Id fallback
        if (xUserId != null) {
            User activeUser = getActiveUserById(xUserId);
            if (activeUser != null) {
                return activeUser.getId();
            }
        }

        // 3~4) 개발 fallback
        Long fallbackUserId = resolveDevFallbackUserId();
        if (fallbackUserId != null) {
            return fallbackUserId;
        }

        throw new AuthenticationCredentialsNotFoundException("인증 사용자 정보를 확인할 수 없습니다.");
    }

    private Long resolveFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String name = authentication.getName();
        if (name == null || "anonymousUser".equals(name)) {
            return null;
        }

        return resolveByPrincipalName(name);
    }

    private Long resolveFromPrincipal(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return null;
        }
        return resolveByPrincipalName(principal.getName());
    }

    private Long resolveByPrincipalName(String principalName) {
        try {
            // Principal 이름이 숫자 문자열인 경우를 먼저 허용 (예: "1")
            long parsed = Long.parseLong(principalName);
            User activeUser = getActiveUserById(parsed);
            if (activeUser != null) {
                return activeUser.getId();
            }
        } catch (NumberFormatException ignored) {
            // ignore
        }

        User user = userRepository.findByUsername(principalName).orElse(null);
        if (user == null) {
            return null;
        }
        if (!"N".equalsIgnoreCase(user.getDeletedYn())) {
            return null;
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            return null;
        }
        return user.getId();
    }

    private Long resolveDevFallbackUserId() {
        if (!allowDevFallback) {
            return null;
        }
        // 설정값(app.auth.dev-user-id)이 실제 존재하면 우선 사용
        if (devUserId != null) {
            User configured = getActiveUserById(devUserId);
            if (configured != null) {
                return configured.getId();
            }
        }
        // 설정값이 없거나 잘못된 경우, 첫 활성 유저를 자동 사용
        return userRepository.findFirstByDeletedYnOrderByIdAsc("N")
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .map(User::getId)
                .orElse(null);
    }

    private User getActiveUserById(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> "N".equalsIgnoreCase(user.getDeletedYn()))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElse(null);
    }
}
