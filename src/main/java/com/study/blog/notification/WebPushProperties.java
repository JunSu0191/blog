package com.study.blog.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebPushProperties {

    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private final String defaultIconUrl;
    private final String defaultBadgeUrl;

    public WebPushProperties(@Value("${app.push.vapid.public-key:}") String publicKey,
                             @Value("${app.push.vapid.private-key:}") String privateKey,
                             @Value("${app.push.vapid.subject:}") String subject,
                             @Value("${app.push.default-icon-url:}") String defaultIconUrl,
                             @Value("${app.push.default-badge-url:}") String defaultBadgeUrl) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
        this.defaultIconUrl = defaultIconUrl;
        this.defaultBadgeUrl = defaultBadgeUrl;
    }

    public boolean isConfigured() {
        return !publicKey.isBlank() && !privateKey.isBlank() && !subject.isBlank();
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getSubject() {
        return subject;
    }

    public String getDefaultIconUrl() {
        return defaultIconUrl;
    }

    public String getDefaultBadgeUrl() {
        return defaultBadgeUrl;
    }
}
