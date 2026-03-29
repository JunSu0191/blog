package com.study.blog.chat;

import java.time.LocalDateTime;

public interface ConversationSummaryProjection {
    Long getConversationId();

    String getConversationType();

    String getTitle();

    String getDirectKey();

    Long getLastMessageId();

    String getLastMessageBody();

    LocalDateTime getLastMessageCreatedAt();

    Long getLastSenderId();

    Long getUnreadMessageCount();

    LocalDateTime getHiddenAt();
}
