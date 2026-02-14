-- V12__create_comment_likes.sql
-- 댓글 좋아요 테이블
CREATE TABLE comment_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (comment_id) REFERENCES comments(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_comment_likes (comment_id, user_id)
);

CREATE INDEX idx_comment_likes_comment_id ON comment_likes(comment_id);
CREATE INDEX idx_comment_likes_user_id ON comment_likes(user_id);

-- 댓글에 좋아요 수 컬럼 추가
ALTER TABLE comments
ADD COLUMN like_count BIGINT DEFAULT 0;