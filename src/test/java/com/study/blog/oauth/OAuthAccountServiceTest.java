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
    private PendingOAuthSignupRepository pendingOAuthSignupRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private OAuthAccountService oauthAccountService;

    @BeforeEach
    void setUp() {
        oauthAccountService = new OAuthAccountService(
                oauthAccountRepository,
                pendingOAuthSignupRepository,
                userRepository,
                passwordEncoder,
                1800);
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

        OAuthAccountService.OAuthLoginResult result = oauthAccountService.loginOrPrepareSignup(
                new OAuthUserInfo(OAuthProvider.GOOGLE, "google-123", "user@gmail.com", "Google User"));

        assertThat(result.user().getId()).isEqualTo(1L);
        verify(oauthAccountRepository).save(account);
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void shouldCreatePendingSignupForNewSocialUser() {
        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.KAKAO, "999"))
                .thenReturn(Optional.empty());
        when(pendingOAuthSignupRepository.saveAndFlush(any(PendingOAuthSignup.class))).thenAnswer(invocation -> {
            PendingOAuthSignup saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        OAuthAccountService.OAuthLoginResult result = oauthAccountService.loginOrPrepareSignup(
                new OAuthUserInfo(OAuthProvider.KAKAO, "999", null, "sample_kakao_user"));

        assertThat(result.needsProfileSetup()).isTrue();
        assertThat(result.signupToken()).isNotBlank();
        verify(pendingOAuthSignupRepository).saveAndFlush(any(PendingOAuthSignup.class));
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

        assertThatThrownBy(() -> oauthAccountService.loginOrPrepareSignup(
                new OAuthUserInfo(OAuthProvider.GOOGLE, "google-123", "user@gmail.com", "Google User")))
                .isInstanceOf(OAuth2LoginException.class)
                .hasMessageContaining("정지된 계정");
    }

    @Test
    void shouldCompletePendingSignup() {
        PendingOAuthSignup pendingSignup = PendingOAuthSignup.builder()
                .id(10L)
                .provider(OAuthProvider.NAVER)
                .providerUserId("naver-123")
                .email("user@naver.com")
                .name("Naver User")
                .signupToken("signup-token")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(pendingOAuthSignupRepository.findBySignupToken("signup-token")).thenReturn(Optional.of(pendingSignup));
        when(oauthAccountRepository.findByProviderAndProviderUserId(OAuthProvider.NAVER, "naver-123"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByUsername("tlswnstn21")).thenReturn(false);
        when(userRepository.existsByNickname("신준수")).thenReturn(false);
        when(userRepository.existsByEmail("user@naver.com")).thenReturn(false);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(200L);
            return saved;
        });
        when(oauthAccountRepository.saveAndFlush(any(OAuthAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = oauthAccountService.completeSignup("signup-token", "tlswnstn21", "신준수");

        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getUsername()).isEqualTo("tlswnstn21");
        assertThat(result.getNickname()).isEqualTo("신준수");
        verify(pendingOAuthSignupRepository).delete(pendingSignup);
        verify(oauthAccountRepository).saveAndFlush(any(OAuthAccount.class));
    }

    @Test
    void shouldRejectExpiredPendingSignup() {
        PendingOAuthSignup pendingSignup = PendingOAuthSignup.builder()
                .id(10L)
                .provider(OAuthProvider.NAVER)
                .providerUserId("naver-123")
                .signupToken("signup-token")
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .build();

        when(pendingOAuthSignupRepository.findBySignupToken("signup-token")).thenReturn(Optional.of(pendingSignup));

        assertThatThrownBy(() -> oauthAccountService.completeSignup("signup-token", "tlswnstn21", "신준수"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("만료");

        verify(pendingOAuthSignupRepository).delete(pendingSignup);
    }
}
