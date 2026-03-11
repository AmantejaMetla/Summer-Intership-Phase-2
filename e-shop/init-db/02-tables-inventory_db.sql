-- Run this on the demo laptop after creating databases.
-- Use database: inventory_db (for inventory-service)
USE inventory_db;

CREATE TABLE IF NOT EXISTS stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    UNIQUE KEY uk_product_id (product_id)
);
