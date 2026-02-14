-- V11__add_view_and_like_counts_to_posts.sql
-- 게시글에 조회수와 좋아요 수 컬럼 추가
ALTER TABLE posts
ADD COLUMN view_count BIGINT DEFAULT 0,
ADD COLUMN like_count BIGINT DEFAULT 0;