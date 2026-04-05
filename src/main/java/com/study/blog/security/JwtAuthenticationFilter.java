package com.study.blog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> JWT_SKIP_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/check-username",
            "/api/auth/check-nickname",
            "/api/auth/oauth/signup/pending",
            "/api/auth/oauth/signup/complete"
    );

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        log.info("JwtAuthenticationFilter path = {}", path);

        // 공개 엔드포인트는 필터 스킵
        if (shouldSkip(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authorization 헤더가 없으면 차단하지 말고 다음 필터로 진행
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            // 토큰 검증 로직 (예: jwtUtil.validateToken 등)
            if (!jwtUtil.validateToken(token)) {
                log.warn("JWT validation failed: path={}, reason=invalid_or_expired", path);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            UsernamePasswordAuthenticationToken auth = jwtUtil.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception ex) {
            // 검증 실패 시 401로 응답하거나 그냥 다음 필터로 넘겨서 최종적으로 인증 안된 상태로 처리
            log.warn("JWT authentication failed: path={}, error={}: {}", path,
                    ex.getClass().getSimpleName(), ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(String path) {
        return JWT_SKIP_PATHS.contains(path)
                || path.startsWith("/h2-console/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/api/auth/find-id/")
                || path.startsWith("/api/auth/reset-password/")
                || path.startsWith("/api/verifications/");
    }
}
