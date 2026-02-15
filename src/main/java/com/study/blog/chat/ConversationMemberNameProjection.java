package com.study.blog.chat;

public interface ConversationMemberNameProjection {

    Long getConversationId();

    Long getUserId();

    String getUserName();
}
