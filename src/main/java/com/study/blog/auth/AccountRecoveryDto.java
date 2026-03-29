package com.study.blog.auth;

import com.study.blog.verification.VerificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

public class AccountRecoveryDto {

    @Data
    public static class RequestVerification {
        @NotNull
        private VerificationChannel channel;

        @NotBlank
        private String target;
    }

    @Data
    public static class ConfirmVerification {
        @NotNull
        private Long verificationId;

        @NotBlank
        private String code;
    }

    @Data
    public static class FindIdResponse {
        private String maskedUsername;
    }

    @Data
    public static class ResetPasswordConfirmResponse {
        private String resetToken;
        private LocalDateTime expiresAt;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank
        private String resetToken;

        @NotBlank
        private String newPassword;
    }
}
