-- =============================================================================
-- Support message thread table for ticket chat (web + WhatsApp bridge).
-- Run this on MySQL setup if table is not auto-created by JPA.
-- =============================================================================

USE eshop;

CREATE TABLE IF NOT EXISTS support_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  ticket_id BIGINT NOT NULL,
  sender_user_id BIGINT NOT NULL,
  sender_type VARCHAR(32) NOT NULL,
  channel VARCHAR(24) NOT NULL,
  body TEXT NOT NULL,
  external_message_id VARCHAR(120),
  created_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_support_messages_ticket_id ON support_messages(ticket_id);
CREATE INDEX idx_support_messages_created_at ON support_messages(created_at);
