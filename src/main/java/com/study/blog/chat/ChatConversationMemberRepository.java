package com.study.blog.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatConversationMemberRepository extends JpaRepository<ChatConversationMember, ChatConversationMemberId> {

    boolean existsByConversation_IdAndUser_Id(Long conversationId, Long userId);

    Optional<ChatConversationMember> findByConversation_IdAndUser_Id(Long conversationId, Long userId);

    @Query("select m.user.id from ChatConversationMember m where m.conversation.id = :conversationId")
    List<Long> findUserIdsByConversationId(@Param("conversationId") Long conversationId);

    @Query("""
            select
                m.conversation.id as conversationId,
                m.user.id as userId,
                case
                    when m.user.name is null or trim(m.user.name) = '' then m.user.username
                    else m.user.name
                end as userName
            from ChatConversationMember m
            where m.conversation.id in :conversationIds
            order by m.conversation.id asc, m.joinedAt asc, m.user.id asc
            """)
    List<ConversationMemberNameProjection> findMemberNamesByConversationIds(
            @Param("conversationIds") Collection<Long> conversationIds);

    @Query("""
            select
                m.conversation.id as conversationId,
                m.user.id as userId,
                case
                    when m.user.name is null or trim(m.user.name) = '' then m.user.username
                    else m.user.name
                end as userName
            from ChatConversationMember m
            where m.conversation.id = :conversationId
            order by m.joinedAt asc, m.user.id asc
            """)
    List<ConversationMemberNameProjection> findMemberNamesByConversationId(@Param("conversationId") Long conversationId);

    @Query(value = """
            select count(1)
            from chat_message m
            join chat_conversation_member cm
              on cm.conversation_id = m.conversation_id
             and cm.user_id = :userId
            where m.conversation_id = :conversationId
              and m.sender_id <> :userId
              and (cm.last_read_message_id is null or m.id > cm.last_read_message_id)
            """, nativeQuery = true)
    long countUnreadMessages(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Query(value = """
            select count(1)
            from chat_message m
            join chat_conversation_member cm
              on cm.conversation_id = m.conversation_id
             and cm.user_id = :userId
            where m.sender_id <> :userId
              and (cm.last_read_message_id is null or m.id > cm.last_read_message_id)
            """, nativeQuery = true)
    long countTotalUnreadMessages(@Param("userId") Long userId);

    long countByConversation_Id(Long conversationId);
}
