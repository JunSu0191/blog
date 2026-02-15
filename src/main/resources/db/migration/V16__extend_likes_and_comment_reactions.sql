-- V16__extend_likes_and_comment_reactions.sql
-- likes 테이블 소프트 삭제 컬럼 및 댓글 반응 타입 확장

ALTER TABLE post_likes
ADD COLUMN deleted_yn CHAR(1) NOT NULL DEFAULT 'N';

CREATE INDEX idx_post_likes_deleted_yn ON post_likes(deleted_yn);

ALTER TABLE comment_likes
ADD COLUMN deleted_yn CHAR(1) NOT NULL DEFAULT 'N',
ADD COLUMN reaction_type VARCHAR(10) NOT NULL DEFAULT 'LIKE';

CREATE INDEX idx_comment_likes_deleted_yn ON comment_likes(deleted_yn);
CREATE INDEX idx_comment_likes_reaction_type ON comment_likes(reaction_type);

ALTER TABLE comments
ADD COLUMN dislike_count BIGINT DEFAULT 0;
