package com.study.blog.verification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private VerificationCodeRepository verificationCodeRepository;
    @Mock
    private VerificationTargetNormalizer targetNormalizer;
    @Mock
    private VerificationMessageSender messageSender;
    @Mock
    private VerificationRateLimiter rateLimiter;

    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService(
                verificationCodeRepository,
                targetNormalizer,
                messageSender,
                rateLimiter,
                300,
                5,
                60,
                "test-hash-secret",
                true);
    }

    @Test
    void sendShouldReturnVerificationIdAndDebugCode() {
        when(targetNormalizer.normalizeAndValidate(VerificationChannel.SMS, "010-0000-0003"))
                .thenReturn("01000000003");
        when(verificationCodeRepository.findTopByTargetAndPurposeOrderByCreatedAtDesc("01000000003", VerificationPurpose.SIGNUP))
                .thenReturn(Optional.empty());
        when(verificationCodeRepository.save(any(VerificationCode.class))).thenAnswer(invocation -> {
            VerificationCode saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        VerificationService.SendResult result = verificationService.send(
                VerificationPurpose.SIGNUP,
                VerificationChannel.SMS,
                "010-0000-0003",
                "127.0.0.1",
                true);

        assertThat(result.verificationCode().getId()).isEqualTo(1L);
        assertThat(result.verificationCode().getTarget()).isEqualTo("01000000003");
        assertThat(result.debugCode()).isNotBlank();
        verify(messageSender).send(any(VerificationChannel.class), any(String.class), any(String.class));
    }

    @Test
    void confirmShouldIncrementAttemptsWhenCodeMismatched() {
        VerificationCode code = VerificationCode.builder()
                .id(10L)
                .purpose(VerificationPurpose.SIGNUP)
                .channel(VerificationChannel.SMS)
                .target("01000000003")
                .codeHash("expected-hash")
                .attempts(0)
                .resendCount(0)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .build();

        when(verificationCodeRepository.findById(10L)).thenReturn(Optional.of(code));
        when(verificationCodeRepository.save(any(VerificationCode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> verificationService.confirm(10L, "123456", VerificationPurpose.SIGNUP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("인증번호가 일치하지 않습니다.");

        ArgumentCaptor<VerificationCode> captor = ArgumentCaptor.forClass(VerificationCode.class);
        verify(verificationCodeRepository).save(captor.capture());
        assertThat(captor.getValue().getAttempts()).isEqualTo(1);
    }

    @Test
    void confirmShouldFailWhenAttemptsExceeded() {
        VerificationCode code = VerificationCode.builder()
                .id(11L)
                .purpose(VerificationPurpose.SIGNUP)
                .channel(VerificationChannel.SMS)
                .target("01000000003")
                .codeHash("expected-hash")
                .attempts(5)
                .resendCount(0)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .build();

        when(verificationCodeRepository.findById(11L)).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> verificationService.confirm(11L, "123456", VerificationPurpose.SIGNUP))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("시도 횟수를 초과");
    }
}
