package com.study.blog.realtime;

import com.study.blog.chat.dto.ChatDto;
import com.study.blog.notification.dto.NotificationDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompRealtimeEventPublisher implements RealtimeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public StompRealtimeEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publishConversationMessage(Long conversationId, ChatDto.ConversationEvent event) {
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, event);
    }

    @Override
    public void publishConversationAck(String principalName, Long senderId, Long conversationId, ChatDto.MessageAckEvent ack) {
        if (principalName != null && !principalName.isBlank()) {
            messagingTemplate.convertAndSendToUser(principalName,
                    "/queue/conversations/" + conversationId + "/acks", ack);
        }
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/acks/" + senderId, ack);
    }

    @Override
    public void publishNotification(String username, Long userId, NotificationDto.Event event) {
        if (username != null && !username.isBlank()) {
            messagingTemplate.convertAndSendToUser(username, "/queue/notifications", event);
        }
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, event);
    }
}
