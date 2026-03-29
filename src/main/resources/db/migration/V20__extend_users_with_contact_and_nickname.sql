ALTER TABLE users
ADD COLUMN nickname VARCHAR(50) NULL,
ADD COLUMN phone_number VARCHAR(20) NULL,
ADD COLUMN email VARCHAR(255) NULL,
ADD COLUMN phone_verified_at TIMESTAMP NULL,
ADD COLUMN email_verified_at TIMESTAMP NULL;

UPDATE users
SET nickname = username
WHERE nickname IS NULL OR nickname = '';

ALTER TABLE users
MODIFY COLUMN nickname VARCHAR(50) NOT NULL;

CREATE UNIQUE INDEX uk_users_nickname ON users(nickname);
CREATE UNIQUE INDEX uk_users_phone_number ON users(phone_number);
CREATE UNIQUE INDEX uk_users_email ON users(email);
