package com.study.blog.oauth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuthUserInfoExtractor userInfoExtractor;

    public CustomOAuth2UserService(OAuthUserInfoExtractor userInfoExtractor) {
        this.userInfoExtractor = userInfoExtractor;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        try {
            OAuthProvider provider = OAuthProvider.fromRegistrationId(registrationId);
            OAuthUserInfo userInfo = userInfoExtractor.extract(provider, oauth2User.getAttributes());
            return new OAuth2UserPrincipal(oauth2User, userInfo);
        } catch (OAuth2LoginException ex) {
            throw new OAuth2AuthenticationException(new OAuth2Error(ex.getErrorCode()), ex.getMessage(), ex);
        }
    }
}
