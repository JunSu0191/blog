package com.study.blog.chat;

import com.study.blog.security.JwtUtil;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import com.study.blog.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class StompConnectAuthInterceptor implements ChannelInterceptor {
    private static final Logger log = LoggerFactory.getLogger(StompConnectAuthInterceptor.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final boolean allowDevFallback;
    private final Long devUserId;

    public StompConnectAuthInterceptor(JwtUtil jwtUtil,
                                       UserRepository userRepository,
                                       @Value("${app.auth.allow-dev-fallback:false}") boolean allowDevFallback,
                                       @Value("${app.auth.dev-user-id:}") String devUserId) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.allowDevFallback = allowDevFallback;
        this.devUserId = (devUserId == null || devUserId.isBlank()) ? null : Long.parseLong(devUserId);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() != StompCommand.CONNECT) {
            
            return message;
        }

        User resolvedUser = resolveFromToken(accessor)
                .or(() -> resolveFromUserIdHeader(accessor))
                .or(this::resolveFromDevFallback)
                .orElseThrow(() -> {
                    log.warn("stomp connect auth failed: hasAuthorizationHeader={}, hasXUserIdHeader={}, allowDevFallback={}",
                            firstHeader(accessor, "Authorization") != null || firstHeader(accessor, "X-Authorization") != null,
                            firstHeader(accessor, "X-User-Id") != null,
                            allowDevFallback);
                    return new AuthenticationCredentialsNotFoundException("STOMP CONNECT 사용자 인증 실패");
                });

        accessor.setUser(new UsernamePasswordAuthenticationToken(
                resolvedUser.getUsername(),
                null,
                Collections.emptyList()));

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            // CONNECT 직후 세션에 사용자 메타데이터를 남겨서 디버깅/추적에 활용
            sessionAttributes.put("userId", resolvedUser.getId());
            sessionAttributes.put("username", resolvedUser.getUsername());
        }

        log.info("stomp connect authenticated: username={}, userId={}",
                resolvedUser.getUsername(), resolvedUser.getId());
        return message;
    }

    private Optional<User> resolveFromToken(StompHeaderAccessor accessor) {
        String authHeader = firstHeader(accessor, "Authorization");
        if (authHeader == null) {
            authHeader = firstHeader(accessor, "X-Authorization");
        }
        if (authHeader == null) {
            authHeader = firstHeader(accessor, "authorization");
        }
        if (authHeader == null) {
            authHeader = firstHeader(accessor, "x-authorization");
        }

        if (authHeader == null) {
            return Optional.empty();
        }

        String token = extractBearerToken(authHeader);
        if (token == null || token.isBlank()) {
            log.warn("stomp connect token parse failed: authorizationHeader={}", authHeader);
            return Optional.empty();
        }

        if (!jwtUtil.validateToken(token)) {
            log.warn("stomp connect token validation failed");
            return Optional.empty();
        }

        String username = jwtUtil.extractUsername(token);
        if (username == null || username.isBlank()) {
            log.warn("stomp connect token username empty");
            return Optional.empty();
        }
        Optional<User> user = userRepository.findByUsernameAndDeletedYn(username, "N")
                .filter(found -> found.getStatus() == UserStatus.ACTIVE);
        if (user.isEmpty()) {
            log.warn("stomp connect user not found by username: {}", username);
        }
        return user;
    }

    private Optional<User> resolveFromUserIdHeader(StompHeaderAccessor accessor) {
        String userIdHeader = firstHeader(accessor, "X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return Optional.empty();
        }

        try {
            Long userId = Long.parseLong(userIdHeader);
            return userRepository.findById(userId)
                    .filter(user -> "N".equalsIgnoreCase(user.getDeletedYn()))
                    .filter(user -> user.getStatus() == UserStatus.ACTIVE);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private Optional<User> resolveFromDevFallback() {
        if (!allowDevFallback) {
            return Optional.empty();
        }

        if (devUserId != null) {
            Optional<User> byConfiguredId = userRepository.findById(devUserId)
                    .filter(user -> "N".equalsIgnoreCase(user.getDeletedYn()))
                    .filter(user -> user.getStatus() == UserStatus.ACTIVE);
            if (byConfiguredId.isPresent()) {
                return byConfiguredId;
            }
        }

        return userRepository.findFirstByDeletedYnOrderByIdAsc("N")
                .filter(user -> user.getStatus() == UserStatus.ACTIVE);
    }

    private String firstHeader(StompHeaderAccessor accessor, String key) {
        List<String> values = accessor.getNativeHeader(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private String extractBearerToken(String authorizationHeader) {
        String value = authorizationHeader.trim();
        if (value.isBlank()) {
            return null;
        }

        // 표준 케이스: "Bearer <jwt>"
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            value = value.substring(7).trim();
        }

        // 잘못 저장된 케이스 방어: "Bearer Bearer <jwt>"
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            value = value.substring(7).trim();
        }

        // 로컬스토리지에 JSON 문자열 형태로 저장된 토큰("...") 방어
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).trim();
        }

        // URL 인코딩된 prefix 방어: "Bearer%20<token>"
        if (value.regionMatches(true, 0, "Bearer%20", 0, 9)) {
            value = value.substring(9).trim();
        }

        return value;
    }
}
