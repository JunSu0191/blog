package com.study.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * CORS/WebSocket 허용 Origin 패턴을 중앙에서 관리한다.
 */
@Component
public class AllowedOriginsProvider {

    private final List<String> allowedOriginPatterns;

    public AllowedOriginsProvider(@Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.allowedOriginPatterns = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    public List<String> asList() {
        return allowedOriginPatterns;
    }

    public String[] asArray() {
        return allowedOriginPatterns.toArray(String[]::new);
    }
}
