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
        when(userRepository.existsByUsername("tlswnstn111")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("encoded");

        authController.register(new RegisterRequest("tlswnstn111", "pw", "   "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("tlswnstn111");
    }

    @Test
    void registerShouldFallbackNameToUsernameWhenNameIsNull() {
        when(userRepository.existsByUsername("tlswnstn111")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("encoded");

        authController.register(new RegisterRequest("tlswnstn111", "pw", null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("tlswnstn111");
    }

    @Test
    void loginShouldReturnUserNameAsIsWhenPresent() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(org.mockito.Mockito.mock(Authentication.class));
        when(jwtUtil.generateToken("tlswnstn111")).thenReturn("jwt-token");
        when(userRepository.findByUsername("tlswnstn111"))
                .thenReturn(Optional.of(User.builder()
                        .id(6L)
                        .username("tlswnstn111")
                        .name("준수")
                        .password("encoded")
                        .deletedYn("N")
                        .build()));

        ResponseEntity<ApiResponseTemplate<Object>> response = authController.login(
                new LoginRequest("tlswnstn111", "pw"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        AuthResponse.Login data = (AuthResponse.Login) response.getBody().getData();
        assertThat(data.token()).isEqualTo("jwt-token");
        assertThat(data.user().name()).isEqualTo("준수");
    }

    @Test
    void loginShouldFallbackNameToUsernameWhenNameMissing() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(org.mockito.Mockito.mock(Authentication.class));
        when(jwtUtil.generateToken("tlswnstn111")).thenReturn("jwt-token");
        when(userRepository.findByUsername("tlswnstn111"))
                .thenReturn(Optional.of(User.builder()
                        .id(6L)
                        .username("tlswnstn111")
                        .name(null)
                        .password("encoded")
                        .deletedYn("N")
                        .build()));

        ResponseEntity<ApiResponseTemplate<Object>> response = authController.login(
                new LoginRequest("tlswnstn111", "pw"));

        assertThat(response.getBody()).isNotNull();
        AuthResponse.Login data = (AuthResponse.Login) response.getBody().getData();
        assertThat(data.user().name()).isEqualTo("tlswnstn111");
    }

    @Test
    void meShouldFallbackNameToUsernameWhenNameMissing() {
        Authentication authentication = mockAuthentication("tlswnstn111");
        when(userRepository.findByUsername("tlswnstn111"))
                .thenReturn(Optional.of(User.builder()
                        .id(6L)
                        .username("tlswnstn111")
                        .name(" ")
                        .password("encoded")
                        .deletedYn("N")
                        .build()));

        ResponseEntity<ApiResponseTemplate<Object>> response = authController.me(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        AuthResponse.UserSummary data = (AuthResponse.UserSummary) response.getBody().getData();
        assertThat(data.name()).isEqualTo("tlswnstn111");
    }

    private Authentication mockAuthentication(String username) {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        return authentication;
    }
}
