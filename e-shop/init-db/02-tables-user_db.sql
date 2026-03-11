-- Run this on the demo laptop after creating databases.
-- Use database: user_db (for user-service)
USE user_db;

CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auth_id BIGINT NOT NULL,
    full_name VARCHAR(255),
    phone VARCHAR(255),
    address VARCHAR(255)
);
