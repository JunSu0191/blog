UPDATE blog_settings
SET theme_preset = 'minimal'
WHERE theme_preset IS NULL
   OR TRIM(theme_preset) = ''
   OR LOWER(theme_preset) NOT IN ('minimal', 'ocean', 'sunset', 'forest');

UPDATE blog_settings
SET accent_color = '#2563eb'
WHERE accent_color IS NULL
   OR TRIM(accent_color) = ''
   OR accent_color NOT REGEXP '^#[0-9A-Fa-f]{6}$';

UPDATE blog_settings
SET profile_layout = 'default'
WHERE profile_layout IS NULL
   OR TRIM(profile_layout) = ''
   OR LOWER(profile_layout) NOT IN ('default', 'compact', 'centered');

UPDATE blog_settings
SET font_scale = 'md'
WHERE font_scale IS NULL
   OR TRIM(font_scale) = ''
   OR LOWER(font_scale) NOT IN ('sm', 'md', 'lg');

ALTER TABLE blog_settings
    MODIFY COLUMN theme_preset VARCHAR(20) NOT NULL DEFAULT 'minimal',
    MODIFY COLUMN accent_color VARCHAR(7) NOT NULL DEFAULT '#2563eb',
    MODIFY COLUMN profile_layout VARCHAR(20) NOT NULL DEFAULT 'default',
    MODIFY COLUMN font_scale VARCHAR(10) NOT NULL DEFAULT 'md';
