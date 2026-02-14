package com.study.blog.chat;

import com.study.blog.chat.dto.ChatDto;
import com.study.blog.core.security.CurrentUserResolver;
import com.study.blog.realtime.RealtimeEventPublisher;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
/**
 * STOMP 메시지 수신 컨트롤러.
 *
 * 클라이언트가 /app/conversations/{id}/send 로 보낸 프레임을 받아
 * ChatService로 저장 처리 후 ACK 이벤트를 발행한다.
 */
public class ChatWebSocketController {
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final ChatService chatService;
    private final CurrentUserResolver currentUserResolver;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public ChatWebSocketController(ChatService chatService,
                                   CurrentUserResolver currentUserResolver,
                                   RealtimeEventPublisher realtimeEventPublisher) {
        this.chatService = chatService;
        this.currentUserResolver = currentUserResolver;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @MessageMapping("/conversations/{id}/send")
    public void sendMessage(@DestinationVariable("id") Long conversationId,
                            @Valid @Payload ChatDto.SendMessageRequest req,
                            Principal principal,
                            SimpMessageHeaderAccessor headers) {
        // WebSocket에서는 Principal 또는 X-User-Id 헤더로 사용자 식별
        Long xUserId = parseLongHeader(headers.getNativeHeader("X-User-Id"));
        Long senderId = currentUserResolver.resolveFromWebSocket(principal, xUserId);
        log.debug("stomp send requested: conversationId={}, senderId={}, principal={}, clientMsgId={}, type={}",
                conversationId, senderId, principal != null ? principal.getName() : null, req.getClientMsgId(), req.getType());

        ChatService.SendResult result = chatService.sendMessage(senderId, conversationId, req);

        ChatDto.MessageAckEvent ack = new ChatDto.MessageAckEvent();
        ack.setType("MESSAGE_ACK");
        ack.setConversationId(conversationId);
        ack.setMessage(result.message());
        ack.setDeduplicated(result.deduplicated());

        // 요청 보낸 사용자에게 ACK 전달(중복 처리 여부 포함)
        realtimeEventPublisher.publishConversationAck(
                principal != null ? principal.getName() : null,
                senderId,
                conversationId,
                ack);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public Map<String, Object> handleStompException(Exception ex) {
        log.warn("stomp message handling failed: type={}, message={}", ex.getClass().getSimpleName(), ex.getMessage());
        return Map.of(
                "type", "STOMP_ERROR",
                "error", ex.getClass().getSimpleName(),
                "message", ex.getMessage() != null ? ex.getMessage() : "메시지 처리 실패");
    }

    private Long parseLongHeader(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(values.get(0));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
