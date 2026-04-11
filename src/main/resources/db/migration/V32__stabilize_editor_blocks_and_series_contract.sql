ALTER TABLE post_series
    ADD COLUMN cover_image_url VARCHAR(500) NULL;

ALTER TABLE posts
    ADD COLUMN series_assigned_at TIMESTAMP NULL,
    ADD COLUMN series_updated_at TIMESTAMP NULL;
