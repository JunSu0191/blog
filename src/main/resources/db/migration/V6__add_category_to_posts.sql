-- V6__add_category_to_posts.sql
ALTER TABLE posts ADD COLUMN category_id BIGINT;

ALTER TABLE posts ADD CONSTRAINT fk_posts_category_id
    FOREIGN KEY (category_id) REFERENCES categories(id);

CREATE INDEX idx_posts_category_id ON posts(category_id);