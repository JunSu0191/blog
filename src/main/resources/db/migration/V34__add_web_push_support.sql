ALTER TABLE notification
    ADD COLUMN link_url VARCHAR(500) NULL AFTER body;

CREATE TABLE push_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    endpoint VARCHAR(512) NOT NULL,
    p256dh VARCHAR(255) NOT NULL,
    auth VARCHAR(255) NOT NULL,
    user_agent VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_used_at DATETIME(6) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    CONSTRAINT fk_push_subscription_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_push_subscription_endpoint UNIQUE (endpoint)
);

CREATE INDEX idx_push_subscription_user_active ON push_subscription(user_id, is_active);
CREATE INDEX idx_push_subscription_user_updated ON push_subscription(user_id, updated_at DESC);
