package com.study.blog.chat.dto;

import com.study.blog.chat.ConversationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ChatDto {

    @Data
    public static class CreateConversationRequest {
        @NotNull
        private ConversationType type;
        private String title;
        private Long otherUserId;
        private List<Long> memberIds;
    }

    @Data
    public static class ConversationSummaryResponse {
        private Long conversationId;
        private ConversationType type;
        private String title;
        private String directKey;
        private MessageResponse lastMessage;
        private LocalDateTime lastActivityAt;
    }

    @Data
    public static class ChatUserResponse {
        private Long userId;
        private String username;
        private String name;
        private boolean me;
    }

    @Data
    public static class SendMessageRequest {
        @NotBlank
        private String clientMsgId;
        @NotBlank
        private String type;
        private String body;
        private Map<String, Object> metadata;
        private Long replyToMessageId;
    }

    @Data
    public static class MessageResponse {
        private Long id;
        private Long conversationId;
        private Long senderId;
        private String clientMsgId;
        private String type;
        private String body;
        private Map<String, Object> metadata;
        private Long replyToMessageId;
        private LocalDateTime createdAt;
    }

    @Data
    public static class ReadRequest {
        @NotNull
        private Long lastReadMessageId;
    }

    @Data
    public static class ConversationEvent {
        private String type;
        private Long conversationId;
        private MessageResponse message;
    }

    @Data
    public static class MessageAckEvent {
        private String type;
        private Long conversationId;
        private MessageResponse message;
        private boolean deduplicated;
    }
}
