package com.study.blog.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByConversation_IdAndSender_IdAndClientMsgId(Long conversationId, Long senderId, String clientMsgId);

    Optional<ChatMessage> findByIdAndConversation_Id(Long id, Long conversationId);

    @Query("""
            select m from ChatMessage m
            where m.conversation.id = :conversationId
              and (:cursorCreatedAt is null
                   or m.createdAt < :cursorCreatedAt
                   or (m.createdAt = :cursorCreatedAt and m.id < :cursorMessageId))
            order by m.createdAt desc, m.id desc
            """)
    List<ChatMessage> findPageByConversationIdWithCursor(
            @Param("conversationId") Long conversationId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorMessageId") Long cursorMessageId,
            org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("""
            update ChatMessage m
            set m.replyToMessage = null
            where m.conversation.id = :conversationId
              and m.replyToMessage is not null
            """)
    int clearReplyReferences(@Param("conversationId") Long conversationId);

    void deleteByConversation_Id(Long conversationId);
}
