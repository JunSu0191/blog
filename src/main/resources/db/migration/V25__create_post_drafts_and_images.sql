-- V25__create_post_drafts_and_images.sql

CREATE TABLE post_drafts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    author_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(255) NULL,
    content_json LONGTEXT NULL,
    content_html LONGTEXT NULL,
    thumbnail_url VARCHAR(500) NULL,
    category_id BIGINT NULL,
    autosaved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_post_drafts_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT fk_post_drafts_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_post_drafts_author_updated ON post_drafts(author_id, updated_at);
CREATE INDEX idx_post_drafts_autosaved_at ON post_drafts(autosaved_at);

CREATE TABLE post_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uploader_id BIGINT NOT NULL,
    post_id BIGINT NULL,
    draft_id BIGINT NULL,
    url VARCHAR(500) NOT NULL,
    width INT NULL,
    height INT NULL,
    size BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_post_images_uploader FOREIGN KEY (uploader_id) REFERENCES users(id),
    CONSTRAINT fk_post_images_post FOREIGN KEY (post_id) REFERENCES posts(id),
    CONSTRAINT fk_post_images_draft FOREIGN KEY (draft_id) REFERENCES post_drafts(id)
);

CREATE INDEX idx_post_images_uploader_id ON post_images(uploader_id);
CREATE INDEX idx_post_images_post_id ON post_images(post_id);
CREATE INDEX idx_post_images_draft_id ON post_images(draft_id);

