-- V24__enhance_posts_for_rich_editor.sql
-- TipTap 기반 게시글 저장 구조로 posts 확장

ALTER TABLE posts
    ADD COLUMN slug VARCHAR(220) NULL,
    ADD COLUMN subtitle VARCHAR(255) NULL,
    ADD COLUMN excerpt VARCHAR(500) NULL,
    ADD COLUMN content_json LONGTEXT NULL,
    ADD COLUMN content_html LONGTEXT NULL,
    ADD COLUMN thumbnail_url VARCHAR(500) NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN published_at TIMESTAMP NULL,
    ADD COLUMN read_time_minutes INT NOT NULL DEFAULT 0,
    ADD COLUMN toc_json LONGTEXT NULL,
    ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- post_tags 엔티티와 스키마 동기화 (기존 엔티티에 deleted_yn 필드 존재)
ALTER TABLE post_tags
    ADD COLUMN deleted_yn CHAR(1) NOT NULL DEFAULT 'N';

-- 기존 데이터 백필
UPDATE posts
SET content_html = COALESCE(content_html, content)
WHERE content_html IS NULL;

UPDATE posts
SET excerpt = LEFT(TRIM(COALESCE(content_html, content, '')), 500)
WHERE excerpt IS NULL;

UPDATE posts
SET slug = CONCAT('post-', id)
WHERE slug IS NULL OR slug = '';

UPDATE posts
SET status = CASE
    WHEN deleted_yn = 'N' THEN 'PUBLISHED'
    ELSE 'DRAFT'
END
WHERE status IS NULL OR status = '';

UPDATE posts
SET published_at = COALESCE(published_at, created_at)
WHERE status = 'PUBLISHED'
  AND published_at IS NULL;

UPDATE posts
SET updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
WHERE updated_at IS NULL;

ALTER TABLE posts
    MODIFY COLUMN slug VARCHAR(220) NOT NULL;

CREATE UNIQUE INDEX uk_posts_slug ON posts(slug);
CREATE INDEX idx_posts_status_published_at ON posts(status, published_at);
CREATE INDEX idx_posts_view_count ON posts(view_count);
CREATE INDEX idx_posts_like_count ON posts(like_count);
