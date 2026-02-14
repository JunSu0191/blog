-- V9__create_likes_and_views.sql
-- 게시글 좋아요 테이블
CREATE TABLE post_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_post_likes (post_id, user_id)
);

CREATE INDEX idx_post_likes_post_id ON post_likes(post_id);
CREATE INDEX idx_post_likes_user_id ON post_likes(user_id);

-- 게시글 조회수 테이블
CREATE TABLE post_views (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT,  -- 로그인하지 않은 사용자도 조회 가능하므로 NULL 허용
    ip_address VARCHAR(45),  -- IPv4/IPv6 지원
    user_agent TEXT,
    viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_post_views_post_id ON post_views(post_id);
CREATE INDEX idx_post_views_user_id ON post_views(user_id);
CREATE INDEX idx_post_views_viewed_at ON post_views(viewed_at);