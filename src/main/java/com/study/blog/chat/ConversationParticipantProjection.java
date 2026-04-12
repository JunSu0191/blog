package com.study.blog.chat;

public interface ConversationParticipantProjection {

    Long getConversationId();

    Long getUserId();

    String getUsername();

    String getName();

    String getNickname();
}
