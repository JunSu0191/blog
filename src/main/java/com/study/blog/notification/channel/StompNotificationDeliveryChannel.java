package com.study.blog.notification.channel;

import com.study.blog.notification.Notification;
import com.study.blog.notification.dto.NotificationDto;
import com.study.blog.realtime.RealtimeEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class StompNotificationDeliveryChannel implements NotificationDeliveryChannel {

    private final RealtimeEventPublisher realtimeEventPublisher;

    public StompNotificationDeliveryChannel(RealtimeEventPublisher realtimeEventPublisher) {
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Override
    public void deliver(Notification notification, NotificationDto.Event event) {
        realtimeEventPublisher.publishNotification(
                notification.getUser().getUsername(),
                notification.getUser().getId(),
                event);
    }
}
