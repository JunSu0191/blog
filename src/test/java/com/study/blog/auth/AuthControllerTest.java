package com.study.blog.auth;

import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.security.JwtUtil;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(userRepository, passwordEncoder, authenticationManager, jwtUtil);
    }

    @Test
    void registerShouldFallbackNameToUsernameWhenNameIsBlank() {
        when(userRepository.existsByUsername("test")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("encoded");

        authController.register(new RegisterRequest("test", "pw", "   "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("test");
    }

    @Test
    void registerShouldFallbackNameToUsernameWhenNameIsNull() {
        when(userRepository.existsByUsername("test")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("encoded");

        authController.register(new RegisterRequest("test", "pw", null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("test");
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
                        .name("홍길동")
                        .password("encoded")
                        .deletedYn("N")
                        .build()));

        ResponseEntity<ApiResponseTemplate<Object>> response = authController.login(
                new LoginRequest("test", "pw"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        AuthResponse.Login data = (AuthResponse.Login) response.getBody().getData();
        assertThat(data.token()).isEqualTo("jwt-token");
        assertThat(data.user().name()).isEqualTo("홍길동");
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

    private Authentication mockAuthentication(String username) {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        return authentication;
    }
}
