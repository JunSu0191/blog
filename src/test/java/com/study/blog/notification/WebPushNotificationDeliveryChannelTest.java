package com.study.blog.notification;

import com.study.blog.notification.channel.WebPushNotificationDeliveryChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPushNotificationDeliveryChannelTest {

    @Mock
    private PushSubscriptionRepository pushSubscriptionRepository;
    @Mock
    private PushSubscriptionService pushSubscriptionService;
    @Mock
    private PushNotificationGateway pushNotificationGateway;

    private WebPushNotificationDeliveryChannel deliveryChannel;

    @BeforeEach
    void setUp() {
        WebPushProperties properties = new WebPushProperties("public", "private", "mailto:test@example.com", "", "");
        deliveryChannel = new WebPushNotificationDeliveryChannel(
                pushSubscriptionRepository,
                pushSubscriptionService,
                pushNotificationGateway,
                properties);
    }

    @Test
    void deliverShouldMarkSubscriptionUsedOnSuccess() {
        PushSubscription subscription = PushSubscription.builder()
                .id(1L)
                .endpoint("https://push.example/sub")
                .active(true)
                .build();
        Notification notification = Notification.builder()
                .type("CHAT_MESSAGE")
                .title("새 메시지")
                .body("본문")
                .linkUrl("/chat?conversationId=1")
                .payload(Map.of("conversationId", 1L, "messageId", 2L, "senderId", 3L))
                .user(com.study.blog.user.User.builder().id(5L).build())
                .build();

        when(pushSubscriptionRepository.findByUser_IdAndActiveTrue(5L)).thenReturn(List.of(subscription));
        when(pushNotificationGateway.send(eq(subscription), any())).thenReturn(PushSendResult.sent());

        deliveryChannel.deliver(notification, null);

        verify(pushSubscriptionService).markSuccess("https://push.example/sub");
    }

    @Test
    void deliverShouldDeactivateInvalidSubscription() {
        PushSubscription subscription = PushSubscription.builder()
                .id(1L)
                .endpoint("https://push.example/sub")
                .active(true)
                .build();
        Notification notification = Notification.builder()
                .type("FRIEND_REQUEST_RECEIVED")
                .title("친구 요청")
                .body("본문")
                .linkUrl("/chat")
                .payload(Map.of("requestId", 10L, "requesterId", 3L, "targetUserId", 5L, "status", "RECEIVED"))
                .user(com.study.blog.user.User.builder().id(5L).build())
                .build();

        when(pushSubscriptionRepository.findByUser_IdAndActiveTrue(5L)).thenReturn(List.of(subscription));
        when(pushNotificationGateway.send(eq(subscription), any())).thenReturn(PushSendResult.invalidated());

        deliveryChannel.deliver(notification, null);

        verify(pushSubscriptionService).deactivateInvalid("https://push.example/sub");
    }
}
