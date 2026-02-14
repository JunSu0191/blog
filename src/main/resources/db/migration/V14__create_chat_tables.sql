CREATE TABLE chat_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(150) NULL,
    direct_key VARCHAR(100) NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_chat_conversation_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT uk_chat_conversation_direct_key UNIQUE (direct_key)
);

CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    client_msg_id CHAR(36) NOT NULL,
    type VARCHAR(30) NOT NULL,
    body TEXT NULL,
    metadata JSON NULL,
    reply_to_message_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_chat_message_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversation(id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_chat_message_reply FOREIGN KEY (reply_to_message_id) REFERENCES chat_message(id),
    CONSTRAINT uk_chat_message_idempotent UNIQUE (conversation_id, sender_id, client_msg_id)
);

CREATE TABLE chat_conversation_member (
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_read_message_id BIGINT NULL,
    last_read_at DATETIME(6) NULL,
    PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT fk_chat_member_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversation(id),
    CONSTRAINT fk_chat_member_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_chat_member_last_read_message FOREIGN KEY (last_read_message_id) REFERENCES chat_message(id)
);

CREATE INDEX idx_chat_message_conv_created_id ON chat_message(conversation_id, created_at DESC, id DESC);
CREATE INDEX idx_chat_member_user ON chat_conversation_member(user_id, conversation_id);
