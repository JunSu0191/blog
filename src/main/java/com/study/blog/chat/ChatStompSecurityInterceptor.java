package com.study.blog.chat;

import com.study.blog.core.security.CurrentUserResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
/**
 * STOMP 인바운드 보안 인터셉터.
 *
 * SEND/SUBSCRIBE 시 대상 대화방 ID를 추출해서
 * "해당 사용자가 실제 멤버인지"를 검사한다.
 */
public class ChatStompSecurityInterceptor implements ChannelInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ChatStompSecurityInterceptor.class);

    private static final Pattern APP_SEND_PATTERN = Pattern.compile("^/app/conversations/(\\d+)/send$");
    private static final Pattern TOPIC_SUBSCRIBE_PATTERN = Pattern.compile("^/topic/conversations/(\\d+)$");

    private final CurrentUserResolver currentUserResolver;
    private final ChatConversationMemberRepository conversationMemberRepository;

    public ChatStompSecurityInterceptor(CurrentUserResolver currentUserResolver,
                                        ChatConversationMemberRepository conversationMemberRepository) {
        this.currentUserResolver = currentUserResolver;
        this.conversationMemberRepository = conversationMemberRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        // 채팅 권한 검증이 필요한 명령은 SEND, SUBSCRIBE만 처리
        if (command != StompCommand.SEND && command != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }

        Long conversationId = extractConversationId(command, destination);
        if (conversationId == null) {
            // 채팅 destination이 아니면 통과
            return message;
        }

        Long xUserId = parseLongHeader(accessor.getNativeHeader("X-User-Id"));
        Principal principal = accessor.getUser();
        Long userId = currentUserResolver.resolveFromWebSocket(principal, xUserId);

        // 멤버가 아니면 즉시 차단(403)
        boolean isMember = conversationMemberRepository.existsByConversation_IdAndUser_Id(conversationId, userId);
        if (!isMember) {
            log.warn("stomp access denied: command={}, destination={}, conversationId={}, userId={}, principal={}",
                    command, destination, conversationId, userId, principal != null ? principal.getName() : null);
            // SUBSCRIBE 단계에서 예외를 던지면 클라이언트가 CONNECT 실패처럼 인식할 수 있으므로
            // 권한 없는 구독은 조용히 drop 처리한다.
            if (command == StompCommand.SUBSCRIBE) {
                return null;
            }
            throw new AccessDeniedException("대화방 멤버만 접근할 수 있습니다.");
        }

        log.debug("stomp access granted: command={}, destination={}, conversationId={}, userId={}",
                command, destination, conversationId, userId);

        return message;
    }

    private Long extractConversationId(StompCommand command, String destination) {
        Matcher matcher = (command == StompCommand.SEND)
                ? APP_SEND_PATTERN.matcher(destination)
                : TOPIC_SUBSCRIBE_PATTERN.matcher(destination);

        if (!matcher.matches()) {
            return null;
        }

        return Long.parseLong(matcher.group(1));
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
