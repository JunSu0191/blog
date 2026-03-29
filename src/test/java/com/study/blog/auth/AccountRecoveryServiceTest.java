package com.study.blog.auth;

import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import com.study.blog.user.UserRole;
import com.study.blog.user.UserStatus;
import com.study.blog.verification.VerificationChannel;
import com.study.blog.verification.VerificationCode;
import com.study.blog.verification.VerificationPurpose;
import com.study.blog.verification.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRecoveryServiceTest {

    @Mock
    private VerificationService verificationService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountRecoveryService accountRecoveryService;

    @BeforeEach
    void setUp() {
        accountRecoveryService = new AccountRecoveryService(
                verificationService,
                userRepository,
                passwordResetTokenRepository,
                passwordEncoder,
                900,
                "reset-token-secret");
    }

    @Test
    void confirmFindIdShouldReturnMaskedUsername() {
        VerificationCode verificationCode = VerificationCode.builder()
                .id(10L)
                .purpose(VerificationPurpose.FIND_ID)
                .channel(VerificationChannel.EMAIL)
                .target("sample.user@example.test")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verifiedAt(LocalDateTime.now())
                .codeHash("hash")
                .build();
        User user = User.builder()
                .id(1L)
                .username("tester123")
                .nickname("tester")
                .password("encoded")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .deletedYn("N")
                .build();

        when(verificationService.confirm(10L, "123456", VerificationPurpose.FIND_ID)).thenReturn(verificationCode);
        when(userRepository.findByEmailAndDeletedYn("sample.user@example.test", "N")).thenReturn(Optional.of(user));

        String masked = accountRecoveryService.confirmFindId(10L, "123456");
        assertThat(masked).isEqualTo("te******3");
    }

    @Test
    void resetPasswordShouldRejectUsedToken() {
        User user = User.builder()
                .id(1L)
                .username("tester")
                .nickname("tester")
                .password("old")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .deletedYn("N")
                .build();
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .usedAt(LocalDateTime.now())
                .build();
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> accountRecoveryService.resetPassword("raw-token", "new-password"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 사용된");
    }

    @Test
    void resetPasswordShouldMarkTokenAsUsed() {
        User user = User.builder()
                .id(1L)
                .username("tester")
                .nickname("tester")
                .password("old")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .deletedYn("N")
                .build();
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .tokenHash("hash")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .usedAt(null)
                .build();
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");

        accountRecoveryService.resetPassword("raw-token", "new-password");

        assertThat(token.getUsedAt()).isNotNull();
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(passwordResetTokenRepository).save(token);
        verify(userRepository).save(user);
    }

    @Test
    void confirmResetPasswordShouldIssueOneTimeToken() {
        VerificationCode verificationCode = VerificationCode.builder()
                .id(22L)
                .purpose(VerificationPurpose.RESET_PASSWORD)
                .channel(VerificationChannel.SMS)
                .target("01000000002")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verifiedAt(LocalDateTime.now())
                .codeHash("hash")
                .build();
        User user = User.builder()
                .id(2L)
                .username("tester")
                .nickname("tester")
                .password("old")
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .deletedYn("N")
                .phoneNumber("01000000002")
                .build();

        when(verificationService.confirm(22L, "123456", VerificationPurpose.RESET_PASSWORD)).thenReturn(verificationCode);
        when(userRepository.findByPhoneNumberAndDeletedYn("01000000002", "N")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountRecoveryService.ResetTokenResult result = accountRecoveryService.confirmResetPassword(22L, "123456");

        assertThat(result.resetToken()).isNotBlank();
        assertThat(result.expiresAt()).isAfter(LocalDateTime.now());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }
}
