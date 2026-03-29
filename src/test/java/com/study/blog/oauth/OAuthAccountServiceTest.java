package com.study.blog.oauth;

import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import com.study.blog.user.UserRole;
import com.study.blog.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthAccountServiceTest {

    @Mock
    private OAuthAccountRepository oauthAccountRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private OAuthAccountService oauthAccountService;

    @BeforeEach
    void setUp() {
        oauthAccountService = new OAuthAccountService(oauthAccountRepository, userRepository, passwordEncoder);
    }

    @Test
    void shouldReuseExistingOAuthAccount() {
        User existingUser = User.builder()
                .id(1L)
                .username("google_user")
                .name("Google User")
                .password("encoded")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .deletedYn("N")
                .build();
        OAuthAccount account = OAuthAccount.builder()
                .id(10L)
                .user(existingUser)
                .provider(OAuthProvider.GOOGLE)
                .providerUserId("google-123")
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .build();

        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(account));

        User result = oauthAccountService.loginOrRegister(
                new OAuthUserInfo(OAuthProvider.GOOGLE, "google-123", "user@gmail.com", "Google User"));

        assertThat(result.getId()).isEqualTo(1L);
        verify(oauthAccountRepository).save(account);
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void shouldCreateUserAndOAuthAccountForNewSocialUser() {
        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.KAKAO, "999"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByUsername("kakao_999")).thenReturn(false);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        when(oauthAccountRepository.saveAndFlush(any(OAuthAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = oauthAccountService.loginOrRegister(
                new OAuthUserInfo(OAuthProvider.KAKAO, "999", null, "sample_kakao_user"));

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUsername()).isEqualTo("kakao_999");
        assertThat(result.getRole()).isEqualTo(UserRole.USER);
        verify(oauthAccountRepository).saveAndFlush(any(OAuthAccount.class));
    }

    @Test
    void shouldRejectSuspendedUser() {
        User suspended = User.builder()
                .id(1L)
                .username("google_user")
                .password("encoded")
                .status(UserStatus.SUSPENDED)
                .role(UserRole.USER)
                .deletedYn("N")
                .build();
        OAuthAccount account = OAuthAccount.builder()
                .id(10L)
                .user(suspended)
                .provider(OAuthProvider.GOOGLE)
                .providerUserId("google-123")
                .build();

        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-123"))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() -> oauthAccountService.loginOrRegister(
                new OAuthUserInfo(OAuthProvider.GOOGLE, "google-123", "user@gmail.com", "Google User")))
                .isInstanceOf(OAuth2LoginException.class)
                .hasMessageContaining("정지된 계정");
    }

    @Test
    void shouldRecoverWhenOAuthAccountCreatedConcurrently() {
        User createdUser = User.builder()
                .id(200L)
                .username("naver_user")
                .password("encoded")
                .name("Naver User")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .deletedYn("N")
                .build();
        User concurrentUser = User.builder()
                .id(201L)
                .username("existing_naver")
                .password("encoded")
                .name("Existing User")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .deletedYn("N")
                .build();
        OAuthAccount concurrentAccount = OAuthAccount.builder()
                .id(30L)
                .provider(OAuthProvider.NAVER)
                .providerUserId("naver-123")
                .user(concurrentUser)
                .build();

        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.NAVER, "naver-123"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrentAccount));
        when(userRepository.existsByUsername("existing_naver")).thenReturn(false);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded");
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(createdUser);
        when(oauthAccountRepository.saveAndFlush(any(OAuthAccount.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        User result = oauthAccountService.loginOrRegister(
                new OAuthUserInfo(OAuthProvider.NAVER, "naver-123", "existing_naver@naver.com", "Naver User"));

        assertThat(result.getId()).isEqualTo(201L);
        verify(userRepository).delete(createdUser);
        verify(userRepository).flush();
        verify(oauthAccountRepository).save(concurrentAccount);
    }
}
