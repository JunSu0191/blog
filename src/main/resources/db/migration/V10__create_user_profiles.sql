-- V10__create_user_profiles.sql
-- 사용자 프로필 테이블
CREATE TABLE user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    display_name VARCHAR(50),
    bio TEXT,
    avatar_url VARCHAR(500),
    website_url VARCHAR(500),
    location VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);