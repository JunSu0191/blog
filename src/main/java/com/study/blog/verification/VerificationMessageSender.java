package com.study.blog.verification;

public interface VerificationMessageSender {
    void send(VerificationChannel channel, String target, String message);
}
