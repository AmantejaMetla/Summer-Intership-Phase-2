-- Add image_url to products (for seed images and dynamic product images).
USE eshop;
ALTER TABLE products ADD COLUMN image_url VARCHAR(512) NULL;
