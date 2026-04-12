package com.study.blog.chat.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.study.blog.chat.ConversationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatDto {

    @Data
    public static class CreateConversationRequest {
        @NotNull
        private ConversationType type;
        private String title;
        private Long otherUserId;
        @JsonAlias({"userIds", "participantIds", "participantUserIds", "inviteeIds"})
        private List<Long> memberIds;

        @JsonProperty("inviteeId")
        public void setInviteeId(Long inviteeId) {
            addMemberId(inviteeId);
        }

        @JsonProperty("memberId")
        public void setMemberId(Long memberId) {
            addMemberId(memberId);
        }

        @JsonProperty("userId")
        public void setUserId(Long userId) {
            addMemberId(userId);
        }

        @JsonProperty("participantUserId")
        public void setParticipantUserId(Long participantUserId) {
            addMemberId(participantUserId);
        }

        @JsonProperty("participants")
        public void setParticipants(List<?> participants) {
            addMemberReferences(participants);
        }

        @JsonProperty("members")
        public void setMembers(List<?> members) {
            addMemberReferences(members);
        }

        @JsonProperty("users")
        public void setUsers(List<?> users) {
            addMemberReferences(users);
        }

        public void setMemberIds(List<Long> memberIds) {
            if (memberIds == null) {
                this.memberIds = null;
                return;
            }
            LinkedHashSet<Long> normalized = new LinkedHashSet<>();
            memberIds.stream()
                    .filter(Objects::nonNull)
                    .forEach(normalized::add);
            this.memberIds = new ArrayList<>(normalized);
        }

        private void addMemberReferences(List<?> candidates) {
            if (candidates == null) {
                return;
            }
            for (Object candidate : candidates) {
                addMemberId(extractMemberId(candidate));
            }
        }

        private void addMemberId(Long memberId) {
            if (memberId == null) {
                return;
            }
            if (this.memberIds == null) {
                this.memberIds = new ArrayList<>();
            }
            if (!this.memberIds.contains(memberId)) {
                this.memberIds.add(memberId);
            }
        }

        private Long extractMemberId(Object candidate) {
            if (candidate == null) {
                return null;
            }
            if (candidate instanceof Number number) {
                return number.longValue();
            }
            if (candidate instanceof String value) {
                if (value.isBlank()) {
                    return null;
                }
                return Long.parseLong(value.trim());
            }
            if (!(candidate instanceof Map<?, ?> map)) {
                return null;
            }
            Object[] keys = {"userId", "id", "memberId", "participantUserId", "participant_id"};
            for (Object key : keys) {
                Object value = map.get(key);
                if (value instanceof Number number) {
                    return number.longValue();
                }
                if (value instanceof String text && !text.isBlank()) {
                    return Long.parseLong(text.trim());
                }
            }
            return null;
        }
    }

    @Data
    public static class ConversationSummaryResponse {
        private Long conversationId;
        private ConversationType type;
        private String title;
        private String displayTitle;
        private String avatarUrl;
        private String directKey;
        private MessageResponse lastMessage;
        private LocalDateTime lastActivityAt;
        private Long unreadMessageCount;
        private Long participantCount;
        private List<ChatUserResponse> participants;
        private boolean hidden;
    }

    @Data
    public static class ConversationParticipantsResponse {
        private Long participantCount;
        private List<ChatUserResponse> participants;
    }

    @Data
    public static class ChatUserResponse {
        private Long userId;
        private String username;
        private String name;
        private String nickname;
        private String avatarUrl;
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
        private String senderAvatarUrl;
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

    @Data
    public static class ConversationUnreadCountEvent {
        private String type;
        private Long conversationId;
        private Long unreadMessageCount;
        private Long totalUnreadMessageCount;
    }
}
