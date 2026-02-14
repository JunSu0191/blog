package com.study.blog.chat;

import com.study.blog.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_conversation_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversationMember {

    @EmbeddedId
    private ChatConversationMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("conversationId")
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @PrePersist
    void prePersist() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (role == null) {
            role = "MEMBER";
        }
    }
}
