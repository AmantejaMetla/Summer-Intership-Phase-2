USE eshop;

CREATE TABLE IF NOT EXISTS role_applications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  requested_role VARCHAR(30) NOT NULL,
  status VARCHAR(30) NOT NULL,
  full_name VARCHAR(255),
  email VARCHAR(255),
  phone VARCHAR(50),
  government_id VARCHAR(120),
  driving_license VARCHAR(120),
  shop_name VARCHAR(255),
  years_experience INT,
  credentials_summary VARCHAR(500),
  review_notes VARCHAR(500),
  created_at DATETIME(6),
  updated_at DATETIME(6)
);
