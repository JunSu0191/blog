package com.study.blog.chat.dto;

import com.study.blog.chat.ConversationType;
import com.study.blog.chat.social.FriendshipRequestStatus;
import com.study.blog.chat.social.GroupInviteStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

public class ChatContractDto {

    @Data
    public static class FriendResponse {
        private Long userId;
        private String username;
        private String name;
        private String nickname;
        private LocalDateTime friendshipCreatedAt;
    }

    @Data
    public static class CreateFriendRequest {
        @NotNull
        private Long targetUserId;
    }

    @Data
    public static class FriendRequestResponse {
        private Long id;
        private Long requesterId;
        private String requesterUsername;
        private String requesterName;
        private String requesterNickname;
        private Long targetId;
        private String targetUsername;
        private String targetName;
        private String targetNickname;
        private FriendshipRequestStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class FriendRequestCancelResponse {
        private Long requestId;
        private FriendshipRequestStatus status;
        private LocalDateTime canceledAt;
    }

    @Data
    public static class StartDirectRequest {
        @NotNull
        private Long targetUserId;
    }

    @Data
    public static class ThreadSummaryResponse {
        private Long threadId;
        private ConversationType type;
        private String title;
        private String displayTitle;
        private String directKey;
        private ChatDto.MessageResponse lastMessage;
        private LocalDateTime lastActivityAt;
        private Long unreadMessageCount;
        private boolean hidden;
    }

    @Data
    public static class CreateGroupInviteRequest {
        @NotNull
        private Long inviteeId;

        @Min(60)
        @Max(86400 * 7)
        private Long expiresInSeconds;
    }

    @Data
    public static class GroupInviteResponse {
        private Long id;
        private Long groupThreadId;
        private Long inviterId;
        private String inviterUsername;
        private String inviterName;
        private String inviterNickname;
        private Long inviteeId;
        private String inviteeUsername;
        private String inviteeName;
        private String inviteeNickname;
        private GroupInviteStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime respondedAt;
        private LocalDateTime expiresAt;
    }
}
