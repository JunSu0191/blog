package com.study.blog.oauth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class OAuth2UserPrincipal implements OAuth2User {

    private final OAuth2User delegate;
    private final OAuthUserInfo userInfo;

    public OAuth2UserPrincipal(OAuth2User delegate, OAuthUserInfo userInfo) {
        this.delegate = delegate;
        this.userInfo = userInfo;
    }

    public OAuthUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    @Override
    public String getName() {
        return userInfo.provider().name() + ":" + userInfo.providerUserId();
    }
}
