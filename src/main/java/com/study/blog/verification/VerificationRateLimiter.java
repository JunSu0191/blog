package com.study.blog.verification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VerificationRateLimiter {

    private static final long WINDOW_SECONDS = 60L;

    private final int maxPerMinute;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public VerificationRateLimiter(@Value("${app.verification.ip-rate-limit-per-minute:30}") int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    public void checkAndCount(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Counter counter = counters.computeIfAbsent(key, ignored -> new Counter(now, 0));
        synchronized (counter) {
            if (counter.windowStart.plusSeconds(WINDOW_SECONDS).isBefore(now)) {
                counter.windowStart = now;
                counter.count = 0;
            }
            counter.count++;
            if (counter.count > maxPerMinute) {
                throw new IllegalStateException("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
            }
        }
    }

    private static class Counter {
        private LocalDateTime windowStart;
        private int count;

        private Counter(LocalDateTime windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
