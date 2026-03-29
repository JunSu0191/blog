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
                lm.sender_id AS lastSenderId,
                (
                    SELECT COUNT(1)
                    FROM chat_message um
                    WHERE um.conversation_id = c.id
                      AND um.sender_id <> cm.user_id
                      AND um.deleted_at IS NULL
                      AND (cm.last_cleared_at IS NULL OR um.created_at > cm.last_cleared_at)
                      AND (cm.last_read_message_id IS NULL OR um.id > cm.last_read_message_id)
                ) AS unreadMessageCount
                ,
                cm.hidden_at AS hiddenAt
            FROM chat_conversation_member cm
            JOIN chat_conversation c ON c.id = cm.conversation_id
            LEFT JOIN chat_message lm ON lm.id = (
                SELECT m2.id
                FROM chat_message m2
                WHERE m2.conversation_id = c.id
                  AND m2.deleted_at IS NULL
                  AND (cm.last_cleared_at IS NULL OR m2.created_at > cm.last_cleared_at)
                ORDER BY m2.created_at DESC, m2.id DESC
                LIMIT 1
            )
            WHERE cm.user_id = :userId
              AND cm.hidden_at IS NULL
              AND (:conversationType IS NULL OR c.type = :conversationType)
              AND (c.type = 'DIRECT' OR cm.left_at IS NULL)
            ORDER BY COALESCE(lm.created_at, c.created_at) DESC, COALESCE(lm.id, c.id) DESC
            """, nativeQuery = true)
    List<ConversationSummaryProjection> findConversationSummariesByUserId(
            @Param("userId") Long userId,
            @Param("conversationType") String conversationType);
}
