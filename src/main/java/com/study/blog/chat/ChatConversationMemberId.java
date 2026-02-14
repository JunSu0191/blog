package com.study.blog.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChatConversationMemberId implements Serializable {

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "user_id")
    private Long userId;
}
