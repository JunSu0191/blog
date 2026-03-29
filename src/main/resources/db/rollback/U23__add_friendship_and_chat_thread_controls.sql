DROP TABLE IF EXISTS group_invites;
DROP TABLE IF EXISTS friendships;
DROP TABLE IF EXISTS friendship_requests;

DROP INDEX idx_chat_member_unread_cursor ON chat_conversation_member;
DROP INDEX idx_chat_member_user_hidden_left ON chat_conversation_member;

ALTER TABLE chat_message
    DROP COLUMN deleted_at;

ALTER TABLE chat_conversation_member
    DROP COLUMN last_cleared_at,
    DROP COLUMN hidden_at,
    DROP COLUMN left_at;
