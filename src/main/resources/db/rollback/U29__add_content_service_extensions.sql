DROP TABLE IF EXISTS content_reports;
DROP TABLE IF EXISTS admin_recommendations;
DROP TABLE IF EXISTS post_bookmarks;

ALTER TABLE posts
    DROP FOREIGN KEY fk_posts_series;

DROP TABLE IF EXISTS post_series;

DROP INDEX idx_posts_status_visibility_published_at ON posts;
DROP INDEX idx_posts_scheduled_at ON posts;
DROP INDEX idx_posts_series_id ON posts;

ALTER TABLE posts
    DROP COLUMN series_order,
    DROP COLUMN series_id,
    DROP COLUMN scheduled_at,
    DROP COLUMN visibility,
    DROP COLUMN meta_description,
    DROP COLUMN meta_title;

DROP INDEX idx_tags_slug ON tags;
DROP INDEX uk_tags_slug ON tags;

ALTER TABLE tags
    DROP COLUMN updated_at,
    DROP COLUMN description,
    DROP COLUMN slug;
