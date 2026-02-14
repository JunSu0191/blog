package com.study.blog.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatConversationMemberRepository extends JpaRepository<ChatConversationMember, ChatConversationMemberId> {

    boolean existsByConversation_IdAndUser_Id(Long conversationId, Long userId);

    Optional<ChatConversationMember> findByConversation_IdAndUser_Id(Long conversationId, Long userId);

    @Query("select m.user.id from ChatConversationMember m where m.conversation.id = :conversationId")
    List<Long> findUserIdsByConversationId(@Param("conversationId") Long conversationId);
}
