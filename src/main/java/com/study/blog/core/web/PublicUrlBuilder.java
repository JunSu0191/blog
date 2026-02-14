package com.study.blog.core.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 프록시(Nginx/ALB) 환경에서도 공용 URL을 일관되게 생성한다.
 */
@Component
public class PublicUrlBuilder {

    private final String publicBaseUrl;

    public PublicUrlBuilder(@Value("${app.public-base-url:${app.upload.public-base-url:}}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String build(HttpServletRequest request, String path) {
        String normalizedPath = normalizePath(path);
        if (StringUtils.hasText(publicBaseUrl)) {
            String normalizedBaseUrl = trimTrailingSlash(publicBaseUrl);
            return UriComponentsBuilder.fromHttpUrl(normalizedBaseUrl)
                    .path(normalizedPath)
                    .build()
                    .toUriString();
        }

        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(normalizedPath)
                .replaceQuery(null)
                .build()
                .toUriString();
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private String trimTrailingSlash(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
