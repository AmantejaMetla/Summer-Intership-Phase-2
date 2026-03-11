-- =============================================================================
-- Support tickets table for Contact form and Admin support dashboard.
-- Run this on MySQL setup if table is not auto-created by JPA.
-- =============================================================================

USE eshop;

CREATE TABLE IF NOT EXISTS support_tickets (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(180) NOT NULL,
  phone VARCHAR(32),
  order_id VARCHAR(64),
  subject VARCHAR(180) NOT NULL,
  message TEXT NOT NULL,
  screenshot_name VARCHAR(255),
  status VARCHAR(24) NOT NULL DEFAULT 'OPEN',
  admin_notes TEXT,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_support_tickets_user_id ON support_tickets(user_id);
CREATE INDEX idx_support_tickets_status ON support_tickets(status);
