package com.study.blog.realtime;

import com.study.blog.chat.dto.ChatDto;
import com.study.blog.notification.dto.NotificationDto;

public interface RealtimeEventPublisher {

    void publishConversationMessage(Long conversationId, ChatDto.ConversationEvent event);

    void publishConversationAck(String principalName, Long senderId, Long conversationId, ChatDto.MessageAckEvent ack);

    void publishNotification(String username, Long userId, NotificationDto.Event event);
}
