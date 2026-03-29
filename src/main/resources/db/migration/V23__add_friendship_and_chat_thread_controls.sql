ALTER TABLE chat_conversation_member
    ADD COLUMN left_at DATETIME(6) NULL,
    ADD COLUMN hidden_at DATETIME(6) NULL,
    ADD COLUMN last_cleared_at DATETIME(6) NULL;

ALTER TABLE chat_message
    ADD COLUMN deleted_at DATETIME(6) NULL;

CREATE INDEX idx_chat_member_user_hidden_left
    ON chat_conversation_member (user_id, hidden_at, left_at, conversation_id);

CREATE INDEX idx_chat_member_unread_cursor
    ON chat_conversation_member (conversation_id, user_id, last_read_message_id, last_cleared_at);

CREATE TABLE friendship_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requester_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_friendship_requests_requester FOREIGN KEY (requester_id) REFERENCES users(id),
    CONSTRAINT fk_friendship_requests_target FOREIGN KEY (target_id) REFERENCES users(id),
    CONSTRAINT ck_friendship_requests_no_self CHECK (requester_id <> target_id),
    CONSTRAINT uk_friendship_requests_pair_status UNIQUE (requester_id, target_id, status)
);

CREATE INDEX idx_friendship_requests_target_status_created
    ON friendship_requests (target_id, status, created_at DESC);

CREATE INDEX idx_friendship_requests_requester_status_created
    ON friendship_requests (requester_id, status, created_at DESC);

CREATE TABLE friendships (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    friend_user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_friendships_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_friendships_friend_user FOREIGN KEY (friend_user_id) REFERENCES users(id),
    CONSTRAINT ck_friendships_no_self CHECK (user_id <> friend_user_id),
    CONSTRAINT uk_friendships_pair UNIQUE (user_id, friend_user_id)
);

CREATE INDEX idx_friendships_user_created
    ON friendships (user_id, created_at DESC);

CREATE TABLE group_invites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_thread_id BIGINT NOT NULL,
    inviter_id BIGINT NOT NULL,
    invitee_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    responded_at DATETIME(6) NULL,
    expires_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_group_invites_group_thread FOREIGN KEY (group_thread_id) REFERENCES chat_conversation(id),
    CONSTRAINT fk_group_invites_inviter FOREIGN KEY (inviter_id) REFERENCES users(id),
    CONSTRAINT fk_group_invites_invitee FOREIGN KEY (invitee_id) REFERENCES users(id),
    CONSTRAINT ck_group_invites_no_self CHECK (inviter_id <> invitee_id),
    CONSTRAINT uk_group_invites_thread_invitee_status UNIQUE (group_thread_id, invitee_id, status)
);

CREATE INDEX idx_group_invites_invitee_status_expires
    ON group_invites (invitee_id, status, expires_at);

CREATE INDEX idx_group_invites_thread_status_created
    ON group_invites (group_thread_id, status, created_at DESC);
