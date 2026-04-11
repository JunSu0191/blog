package com.study.blog.notification.channel;

import com.study.blog.notification.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WebPushNotificationDeliveryChannel implements NotificationDeliveryChannel {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PushSubscriptionService pushSubscriptionService;
    private final PushNotificationGateway pushNotificationGateway;
    private final WebPushProperties webPushProperties;

    public WebPushNotificationDeliveryChannel(PushSubscriptionRepository pushSubscriptionRepository,
                                              PushSubscriptionService pushSubscriptionService,
                                              PushNotificationGateway pushNotificationGateway,
                                              WebPushProperties webPushProperties) {
        this.pushSubscriptionRepository = pushSubscriptionRepository;
        this.pushSubscriptionService = pushSubscriptionService;
        this.pushNotificationGateway = pushNotificationGateway;
        this.webPushProperties = webPushProperties;
    }

    @Override
    public void deliver(Notification notification, com.study.blog.notification.dto.NotificationDto.Event event) {
        if (!webPushProperties.isConfigured()) {
            return;
        }

        WebPushPayload payload = WebPushPayload.builder()
                .type(notification.getType())
                .title(notification.getTitle())
                .body(notification.getBody())
                .tag(buildTag(notification))
                .linkUrl(notification.getLinkUrl())
                .data(notification.getPayload())
                .icon(blankToNull(webPushProperties.getDefaultIconUrl()))
                .badge(blankToNull(webPushProperties.getDefaultBadgeUrl()))
                .build();

        for (PushSubscription subscription : pushSubscriptionRepository.findByUser_IdAndActiveTrue(notification.getUser().getId())) {
            PushSendResult result = pushNotificationGateway.send(subscription, payload);
            if (result.success()) {
                pushSubscriptionService.markSuccess(subscription.getEndpoint());
            } else if (result.invalidateSubscription()) {
                pushSubscriptionService.deactivateInvalid(subscription.getEndpoint());
            }
        }
    }

    private String buildTag(Notification notification) {
        Map<String, Object> data = notification.getPayload();
        return switch (NotificationType.valueOf(notification.getType())) {
            case POST_COMMENT -> "post-comment:" + data.get("postId") + ":" + data.get("commentId");
            case CHAT_MESSAGE -> "chat-message:" + data.get("conversationId") + ":" + data.get("messageId");
            case FRIEND_REQUEST_RECEIVED,
                    FRIEND_REQUEST_ACCEPTED,
                    FRIEND_REQUEST_REJECTED,
                    FRIEND_REQUEST_CANCELED -> "friend-request:" + data.get("requestId") + ":" + data.get("status")
                    + ":" + firstNonNull(data.get("requesterId"), data.get("targetUserId"));
        };
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
