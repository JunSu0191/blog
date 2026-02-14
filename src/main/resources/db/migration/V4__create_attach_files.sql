-- create_attach_file.sql
CREATE TABLE attach_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    original_name VARCHAR(255),
    stored_name VARCHAR(255),
    path VARCHAR(500),
    deleted_yn CHAR(1) DEFAULT 'N',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id)
);

CREATE INDEX idx_attach_files_post_id ON attach_files(post_id);