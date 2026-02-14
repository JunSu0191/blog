package com.study.blog.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key;
    private final long expirationMs;
    private final UserDetailsService userDetailsService;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms:3600000}") long expirationMs,
                   UserDetailsService userDetailsService) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.userDetailsService = userDetailsService;
    }

    // 토큰에서 username(subject) 추출
    public String extractUsername(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    // 토큰 유효성 검사 (서명 + 만료)
    public boolean validateToken(String token) {
        try {
            Jws<Claims> parsed = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            Date exp = parsed.getBody().getExpiration();
            return exp == null || exp.after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    // 토큰에서 Authentication 생성 (컨트롤러의 요청 처리 전 SecurityContext에 넣을 용도)
    public UsernamePasswordAuthenticationToken getAuthentication(String token) {
        String username = extractUsername(token);
        if (username == null) return null;
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        // credentials는 null로 두고 authorities는 UserDetails에서 가져옵니다
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    // 토큰 생성 유틸 (로그인 시 사용)
    public String generateToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}