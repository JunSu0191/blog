package com.study.blog.verification.dto;

import com.study.blog.verification.VerificationChannel;
import com.study.blog.verification.VerificationPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

public class VerificationDto {

    @Data
    public static class SendRequest {
        @NotNull
        private VerificationPurpose purpose;

        @NotNull
        private VerificationChannel channel;

        @NotBlank
        private String target;
    }

    @Data
    public static class SendResponse {
        private Long verificationId;
        private LocalDateTime expiresAt;
        private Integer resendCount;
        private Long cooldownSeconds;
        private String debugCode;
    }

    @Data
    public static class ConfirmRequest {
        @NotNull
        private Long verificationId;

        @NotBlank
        private String code;
    }

    @Data
    public static class ConfirmResponse {
        private Long verificationId;
        private VerificationPurpose purpose;
        private VerificationChannel channel;
        private String target;
        private LocalDateTime verifiedAt;
    }
}
