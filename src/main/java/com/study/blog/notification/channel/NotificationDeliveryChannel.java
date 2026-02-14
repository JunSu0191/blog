package com.study.blog.notification.channel;

import com.study.blog.notification.Notification;
import com.study.blog.notification.dto.NotificationDto;

/**
 * 알림 전달 채널(예: STOMP, 이메일, 푸시)의 공통 인터페이스.
 */
public interface NotificationDeliveryChannel {

    void deliver(Notification notification, NotificationDto.Event event);
}
