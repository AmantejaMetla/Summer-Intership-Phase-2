-- Run this ONLY if you already created the payments table before the Razorpay update.
-- Adds Razorpay columns without dropping or replacing the table.
-- Use database: eshop (single DB)

USE eshop;

-- Run each line. If you get "Duplicate column name", that column already exists — skip that line.
ALTER TABLE payments ADD COLUMN gateway_order_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN gateway_payment_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN receipt_url VARCHAR(512);
ALTER TABLE payments ADD COLUMN payment_method VARCHAR(50) DEFAULT 'RAZORPAY';
