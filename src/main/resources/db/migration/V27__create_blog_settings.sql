CREATE TABLE blog_settings (
    user_id BIGINT PRIMARY KEY,
    theme_preset VARCHAR(50) NOT NULL DEFAULT 'default',
    accent_color VARCHAR(20) NULL,
    cover_image_url TEXT NULL,
    profile_layout VARCHAR(50) NOT NULL DEFAULT 'classic',
    font_scale VARCHAR(20) NOT NULL DEFAULT 'md',
    show_stats BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_blog_settings_user FOREIGN KEY (user_id) REFERENCES users(id)
);
