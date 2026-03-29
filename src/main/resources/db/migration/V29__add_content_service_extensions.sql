ALTER TABLE tags
    ADD COLUMN slug VARCHAR(220) NULL,
    ADD COLUMN description VARCHAR(500) NULL,
    ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

UPDATE tags
SET slug = LOWER(REPLACE(TRIM(name), ' ', '-'))
WHERE slug IS NULL OR slug = '';

UPDATE tags
SET updated_at = COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
WHERE updated_at IS NULL;

ALTER TABLE tags
    MODIFY COLUMN slug VARCHAR(220) NOT NULL;

CREATE UNIQUE INDEX uk_tags_slug ON tags(slug);
CREATE INDEX idx_tags_slug ON tags(slug);

CREATE TABLE post_series (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL,
    description VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_post_series_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT uk_post_series_slug UNIQUE (slug)
);

CREATE INDEX idx_post_series_owner_id ON post_series(owner_id);
CREATE INDEX idx_post_series_slug ON post_series(slug);

ALTER TABLE posts
    ADD COLUMN meta_title VARCHAR(255) NULL,
    ADD COLUMN meta_description VARCHAR(500) NULL,
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    ADD COLUMN scheduled_at TIMESTAMP NULL,
    ADD COLUMN series_id BIGINT NULL,
    ADD COLUMN series_order INT NULL;

UPDATE posts
SET visibility = 'PUBLIC'
WHERE visibility IS NULL OR visibility = '';

ALTER TABLE posts
    ADD CONSTRAINT fk_posts_series FOREIGN KEY (series_id) REFERENCES post_series(id);

CREATE INDEX idx_posts_series_id ON posts(series_id);
CREATE INDEX idx_posts_scheduled_at ON posts(scheduled_at);
CREATE INDEX idx_posts_status_visibility_published_at ON posts(status, visibility, published_at);

CREATE TABLE post_bookmarks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    bookmarked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_yn CHAR(1) NOT NULL DEFAULT 'N',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_post_bookmarks_post FOREIGN KEY (post_id) REFERENCES posts(id),
    CONSTRAINT fk_post_bookmarks_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_post_bookmarks UNIQUE (post_id, user_id)
);

CREATE INDEX idx_post_bookmarks_user_id ON post_bookmarks(user_id);
CREATE INDEX idx_post_bookmarks_deleted_yn ON post_bookmarks(deleted_yn);
CREATE INDEX idx_post_bookmarks_bookmarked_at ON post_bookmarks(bookmarked_at);

CREATE TABLE admin_recommendations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slot INT NOT NULL,
    post_id BIGINT NOT NULL,
    created_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_recommendations_post FOREIGN KEY (post_id) REFERENCES posts(id),
    CONSTRAINT fk_admin_recommendations_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT uk_admin_recommendations_slot UNIQUE (slot),
    CONSTRAINT uk_admin_recommendations_post UNIQUE (post_id)
);

CREATE INDEX idx_admin_recommendations_slot ON admin_recommendations(slot);

CREATE TABLE content_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL,
    post_id BIGINT NULL,
    comment_id BIGINT NULL,
    reporter_id BIGINT NULL,
    reason VARCHAR(100) NOT NULL,
    description VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resolution_note VARCHAR(1000) NULL,
    resolved_by BIGINT NULL,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_content_reports_post FOREIGN KEY (post_id) REFERENCES posts(id),
    CONSTRAINT fk_content_reports_comment FOREIGN KEY (comment_id) REFERENCES comments(id),
    CONSTRAINT fk_content_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_content_reports_resolved_by FOREIGN KEY (resolved_by) REFERENCES users(id)
);

CREATE INDEX idx_content_reports_status ON content_reports(status);
CREATE INDEX idx_content_reports_target_type ON content_reports(target_type);
CREATE INDEX idx_content_reports_post_id ON content_reports(post_id);
CREATE INDEX idx_content_reports_comment_id ON content_reports(comment_id);
