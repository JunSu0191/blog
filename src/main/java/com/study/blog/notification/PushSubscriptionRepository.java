package com.study.blog.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    Optional<PushSubscription> findByUser_IdAndEndpoint(Long userId, String endpoint);

    List<PushSubscription> findByUser_IdAndActiveTrue(Long userId);
}
