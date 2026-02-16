ALTER TABLE users
ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER',
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
ADD COLUMN must_change_password TINYINT(1) NOT NULL DEFAULT 0;

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);

ALTER TABLE posts
ADD COLUMN deleted_at TIMESTAMP NULL;

CREATE INDEX idx_posts_deleted_at ON posts(deleted_at);

ALTER TABLE comments
ADD COLUMN deleted_at TIMESTAMP NULL;

CREATE INDEX idx_comments_deleted_at ON comments(deleted_at);

UPDATE posts
SET deleted_at = COALESCE(deleted_at, created_at, CURRENT_TIMESTAMP)
WHERE deleted_yn = 'Y'
  AND deleted_at IS NULL;

UPDATE comments
SET deleted_at = COALESCE(deleted_at, updated_at, created_at, CURRENT_TIMESTAMP)
WHERE deleted_yn = 'Y'
  AND deleted_at IS NULL;

CREATE TABLE admin_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_admin_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    before_json LONGTEXT NULL,
    after_json LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_audit_actor_created
    ON admin_audit_log(actor_admin_id, created_at);
CREATE INDEX idx_admin_audit_target
    ON admin_audit_log(target_type, target_id, created_at);
