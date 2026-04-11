package com.study.blog.notification;

import com.study.blog.notification.dto.PushSubscriptionDto;
import com.study.blog.user.User;
import com.study.blog.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;

    public PushSubscriptionService(PushSubscriptionRepository pushSubscriptionRepository,
                                   UserRepository userRepository) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.userRepository = userRepository;
    }

    public SaveResult save(Long userId, PushSubscriptionDto.SaveRequest request) {
        if (request.getKeys() == null) {
            throw new IllegalArgumentException("keys는 필수입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        PushSubscription subscription = pushSubscriptionRepository.findByEndpoint(request.getEndpoint())
                .orElseGet(PushSubscription::new);
        boolean created = subscription.getId() == null;

        subscription.setUser(user);
        subscription.setEndpoint(request.getEndpoint().trim());
        subscription.setP256dh(request.getKeys().getP256dh().trim());
        subscription.setAuth(request.getKeys().getAuth().trim());
        subscription.setUserAgent(normalizeNullable(request.getUserAgent()));
        subscription.setActive(true);
        subscription.setUpdatedAt(LocalDateTime.now());

        PushSubscription saved = pushSubscriptionRepository.save(subscription);
        return new SaveResult(toResponse(saved), created);
    }

    public void delete(Long userId, String endpoint) {
        pushSubscriptionRepository.findByUser_IdAndEndpoint(userId, endpoint.trim())
                .ifPresent(pushSubscriptionRepository::delete);
    }

    public void deactivateInvalid(String endpoint) {
        pushSubscriptionRepository.findByEndpoint(endpoint)
                .ifPresent(subscription -> {
                    subscription.setActive(false);
                    subscription.setUpdatedAt(LocalDateTime.now());
                });
    }

    public void markSuccess(String endpoint) {
        pushSubscriptionRepository.findByEndpoint(endpoint)
                .ifPresent(subscription -> {
                    subscription.setActive(true);
                    subscription.setLastUsedAt(LocalDateTime.now());
                    subscription.setUpdatedAt(LocalDateTime.now());
                });
    }

    private PushSubscriptionDto.Response toResponse(PushSubscription subscription) {
        PushSubscriptionDto.Response response = new PushSubscriptionDto.Response();
        response.setId(subscription.getId());
        response.setEndpoint(subscription.getEndpoint());
        response.setUserAgent(subscription.getUserAgent());
        response.setCreatedAt(subscription.getCreatedAt());
        response.setUpdatedAt(subscription.getUpdatedAt());
        response.setLastUsedAt(subscription.getLastUsedAt());
        response.setActive(subscription.isActive());
        return response;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record SaveResult(PushSubscriptionDto.Response response, boolean created) {
    }
}
