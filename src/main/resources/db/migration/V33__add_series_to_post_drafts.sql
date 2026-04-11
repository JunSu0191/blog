ALTER TABLE post_drafts
    ADD COLUMN series_id BIGINT NULL,
    ADD COLUMN series_order INT NULL;

ALTER TABLE post_drafts
    ADD CONSTRAINT fk_post_drafts_series FOREIGN KEY (series_id) REFERENCES post_series(id);

CREATE INDEX idx_post_drafts_series_id ON post_drafts(series_id);
