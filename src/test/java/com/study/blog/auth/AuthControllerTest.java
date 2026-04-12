package com.study.blog.auth;

import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.oauth.OAuthAccountService;
import com.study.blog.security.JwtUtil;
import com.study.blog.user.UserAvatarService;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import com.study.blog.verification.VerificationChannel;
import com.study.blog.verification.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private VerificationService verificationService;
    @Mock
    private AccountRecoveryService accountRecoveryService;
    @Mock
    private OAuthAccountService oauthAccountService;
    @Mock
    private UserAvatarService userAvatarService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtUtil,
                verificationService,
                accountRecoveryService,
                oauthAccountService,
                userAvatarService);
        lenient().when(userAvatarService.getAvatarUrl(nullable(Long.class))).thenReturn(null);
    }

    @Test
    void registerShouldFallbackNameToUsernameWhenNameIsBlank() {
        when(userRepository.existsByUsername("test")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("01000000001")).thenReturn(false);
        when(verificationService.normalizeTarget(VerificationChannel.SMS, "010-0000-0001")).thenReturn("01000000001");
        when(passwordEncoder.encode("pw")).thenReturn("encoded");

        authController.register(new RegisterRequest("test", "pw", "   ", "nick", null, "010-0000-0001", null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("test");
        assertThat(captor.getValue().getNickname()).isEqualTo("nick");
        assertThat(captor.getValue().getPhoneNumber()).isEqualTo("01000000001");
        assertThat(captor.getValue().getPhoneVerifiedAt()).isNull();
        verify(verificationService, never()).requireVerified(any(), any(), any(), any());
    }

    @Test
    void registerShouldFallbackNameToUsernameWhenNameIsNullWithoutPhoneVerification() {
        when(userRepository.existsByUsername("test")).thenReturn(false);
        when(verificationService.normalizeTarget(VerificationChannel.EMAIL, "sample.user@example.test")).thenReturn("sample.user@example.test");
        when(userRepository.existsByEmail("sample.user@example.test")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("encoded");

        authController.register(new RegisterRequest("test", "pw", null, "nick", "sample.user@example.test", null, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("test");
        assertThat(captor.getValue().getEmail()).isEqualTo("sample.user@example.test");
        assertThat(captor.getValue().getPhoneNumber()).isNull();
    }

    @Test
    void loginShouldReturnUserNameAsIsWhenPresent() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(org.mockito.Mockito.mock(Authentication.class));
        when(jwtUtil.generateToken("test")).thenReturn("jwt-token");
        when(userRepository.findByUsernameAndDeletedYn("test", "N"))
                .thenReturn(Optional.of(User.builder()
                        .id(6L)
                        .username("test")
                        .name("테스트사용자")
                        .password("encoded")
                        .deletedYn("N")
                        .build()));

        ResponseEntity<ApiResponseTemplate<Object>> response = authController.login(
                new LoginRequest("test", "pw"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        AuthResponse.Login data = (AuthResponse.Login) response.getBody().getData();
        assertThat(data.token()).isEqualTo("jwt-token");
        assertThat(data.user().name()).isEqualTo("테스트사용자");
    }

    @Test
    void loginShouldFallbackNameToUsernameWhenNameMissing() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(org.mockito.Mockito.mock(Authentication.class));
        when(jwtUtil.generateToken("test")).thenReturn("jwt-token");
        when(userRepository.findByUsernameAndDeletedYn("test", "N"))
                .thenReturn(Optional.of(User.builder()
                        .id(6L)
                        .username("test")
                        .name(null)
                        .password("encoded")
                        .deletedYn("N")
                        .build()));

        ResponseEntity<ApiResponseTemplate<Object>> response = authController.login(
                new LoginRequest("test", "pw"));

        assertThat(response.getBody()).isNotNull();
        AuthResponse.Login data = (AuthResponse.Login) response.getBody().getData();
        assertThat(data.user().name()).isEqualTo("test");
    }

    @Test
    void meShouldFallbackNameToUsernameWhenNameMissing() {
        Authentication authentication = mockAuthentication("test");
        when(userRepository.findByUsernameAndDeletedYn("test", "N"))
                .thenReturn(Optional.of(User.builder()
                        .id(6L)
                        .username("test")
                        .name(" ")
                        .password("encoded")
                        .deletedYn("N")
                        .build()));

        ResponseEntity<ApiResponseTemplate<Object>> response = authController.me(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        AuthResponse.UserSummary data = (AuthResponse.UserSummary) response.getBody().getData();
        assertThat(data.name()).isEqualTo("test");
    }

    @Test
    void meShouldReturnNullWhenAnonymous() {
        ResponseEntity<ApiResponseTemplate<Object>> response = authController.me(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    void completeOAuthSignupShouldReturnLoginPayload() {
        User user = User.builder()
                .id(9L)
                .username("tlswnstn21")
                .nickname("신준수")
                .name("신준수")
                .password("encoded")
                .deletedYn("N")
                .build();
        when(oauthAccountService.completeSignup("signup-token", "tlswnstn21", "신준수")).thenReturn(user);
        when(jwtUtil.generateToken("tlswnstn21")).thenReturn("jwt-token");

        ResponseEntity<ApiResponseTemplate<Object>> response = authController.completeOAuthSignup(
                new OAuthSignupCompleteRequest("signup-token", "tlswnstn21", "신준수"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthResponse.Login data = (AuthResponse.Login) response.getBody().getData();
        assertThat(data.token()).isEqualTo("jwt-token");
        assertThat(data.user().username()).isEqualTo("tlswnstn21");
    }

    @Test
    void meShouldIncludeAvatarUrl() {
        Authentication authentication = mockAuthentication("test");
        when(userRepository.findByUsernameAndDeletedYn("test", "N"))
                .thenReturn(Optional.of(User.builder()
                        .id(6L)
                        .username("test")
                        .name("tester")
                        .password("encoded")
                        .deletedYn("N")
                        .build()));
        when(userAvatarService.getAvatarUrl(6L)).thenReturn("https://cdn.example.com/avatar.png");

        ResponseEntity<ApiResponseTemplate<Object>> response = authController.me(authentication);

        AuthResponse.UserSummary data = (AuthResponse.UserSummary) response.getBody().getData();
        assertThat(data.avatarUrl()).isEqualTo("https://cdn.example.com/avatar.png");
    }

    private Authentication mockAuthentication(String username) {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        return authentication;
    }
}
