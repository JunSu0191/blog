-- V26__enhance_categories_with_slug.sql
-- 카테고리 slug/updated_at 및 관련 인덱스 보강

ALTER TABLE categories
    ADD COLUMN slug VARCHAR(220) NULL,
    ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

UPDATE categories
SET slug = CONCAT('category-', id)
WHERE slug IS NULL OR slug = '';

UPDATE categories
SET updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
WHERE updated_at IS NULL;

ALTER TABLE categories
    MODIFY COLUMN slug VARCHAR(220) NOT NULL;

CREATE UNIQUE INDEX uk_categories_slug ON categories(slug);

CREATE INDEX idx_post_drafts_category_id ON post_drafts(category_id);

