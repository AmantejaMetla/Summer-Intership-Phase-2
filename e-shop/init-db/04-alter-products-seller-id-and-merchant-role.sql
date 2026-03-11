-- Run this if you already have the eshop database and products table (add merchant role + seller_id).
USE eshop;

-- Add merchant role if not present
INSERT IGNORE INTO roles (role_name) VALUES ('merchant');

-- Add seller_id to products (for merchant-owned products)
ALTER TABLE products ADD COLUMN seller_id BIGINT NULL;
