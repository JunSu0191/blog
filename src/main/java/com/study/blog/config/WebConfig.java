package com.study.blog.config;

import org.springframework.http.CacheControl;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String[] allowedOrigins;

    public WebConfig(AllowedOriginsProvider allowedOriginsProvider) {
        this.allowedOrigins = allowedOriginsProvider.asArray();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        CacheControl cacheControl = CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic();

        // 신규 경로
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:./upload/")
                .setCacheControl(cacheControl);

        // 이전 경로 호환
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./upload/")
                .setCacheControl(cacheControl);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/upload/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "HEAD", "OPTIONS")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/uploads/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "HEAD", "OPTIONS")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
