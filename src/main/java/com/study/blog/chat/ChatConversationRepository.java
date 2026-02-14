package com.study.blog.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findByDirectKey(String directKey);

    @Query(value = """
            SELECT
                c.id AS conversationId,
                c.type AS conversationType,
                c.title AS title,
                c.direct_key AS directKey,
                lm.id AS lastMessageId,
                lm.body AS lastMessageBody,
                lm.created_at AS lastMessageCreatedAt,
                lm.sender_id AS lastSenderId
            FROM chat_conversation_member cm
            JOIN chat_conversation c ON c.id = cm.conversation_id
            LEFT JOIN chat_message lm ON lm.id = (
                SELECT m2.id
                FROM chat_message m2
                WHERE m2.conversation_id = c.id
                ORDER BY m2.created_at DESC, m2.id DESC
                LIMIT 1
            )
            WHERE cm.user_id = :userId
            ORDER BY COALESCE(lm.created_at, c.created_at) DESC, COALESCE(lm.id, c.id) DESC
            """, nativeQuery = true)
    List<ConversationSummaryProjection> findConversationSummariesByUserId(@Param("userId") Long userId);
}
