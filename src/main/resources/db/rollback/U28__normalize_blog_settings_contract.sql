ALTER TABLE blog_settings
    MODIFY COLUMN theme_preset VARCHAR(50) NOT NULL DEFAULT 'default',
    MODIFY COLUMN accent_color VARCHAR(20) NULL,
    MODIFY COLUMN profile_layout VARCHAR(50) NOT NULL DEFAULT 'classic',
    MODIFY COLUMN font_scale VARCHAR(20) NOT NULL DEFAULT 'md';
