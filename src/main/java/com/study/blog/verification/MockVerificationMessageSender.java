package com.study.blog.verification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockVerificationMessageSender implements VerificationMessageSender {

    @Override
    public void send(VerificationChannel channel, String target, String message) {
        // Plain OTP code must not be logged.
        log.info("Mock verification send requested. channel={}, target={}",
                channel.name(), maskTarget(target));
    }

    private String maskTarget(String target) {
        if (target == null || target.isBlank()) {
            return "***";
        }
        int visible = Math.min(3, target.length());
        String suffix = target.substring(target.length() - visible);
        return "***" + suffix;
    }
}
