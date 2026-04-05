CREATE TABLE pending_oauth_signups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(191) NOT NULL,
    email VARCHAR(255) NULL,
    name VARCHAR(100) NULL,
    signup_token VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_pending_oauth_signups_token UNIQUE (signup_token),
    CONSTRAINT uk_pending_oauth_signups_provider_user UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_pending_oauth_signups_expires_at ON pending_oauth_signups(expires_at);
