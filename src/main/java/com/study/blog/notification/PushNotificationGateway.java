package com.study.blog.notification;

public interface PushNotificationGateway {

    PushSendResult send(PushSubscription subscription, WebPushPayload payload);
}
