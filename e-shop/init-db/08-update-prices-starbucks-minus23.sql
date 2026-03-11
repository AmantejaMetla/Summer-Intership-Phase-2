-- =============================================================================
-- Update existing product prices to realistic INR levels.
-- Pricing baseline: Starbucks-style ranges, then reduced by ~23% for E-Shop.
-- Safe-update compatible version (works when SQL_SAFE_UPDATES=1).
-- =============================================================================

USE eshop;

CREATE TEMPORARY TABLE product_price_updates (
  product_name VARCHAR(255) PRIMARY KEY,
  new_price DECIMAL(10,2) NOT NULL
);

INSERT INTO product_price_updates (product_name, new_price) VALUES
('Cappuccino (Medium)', 255.00),
('Cappuccino (Large)', 285.00),
('Latte (Medium)', 250.00),
('Latte (Large)', 280.00),
('Flat White', 245.00),
('Caramel Macchiato (Medium)', 275.00),
('Caramel Macchiato (Large)', 305.00),
('Espresso', 190.00),
('Mocha (Medium)', 270.00),
('Mocha (Large)', 300.00),
('White Mocha (Medium)', 275.00),
('White Mocha (Large)', 305.00),
('Hot Chocolate (Medium)', 250.00),
('Hot Chocolate (Large)', 280.00),
('Cold Coffee (Medium)', 235.00),
('Cold Coffee (Large)', 265.00),
('Cold Mocha (Medium)', 260.00),
('Cold Mocha (Large)', 290.00),
('Iced Tea (Medium)', 180.00),
('Iced Tea (Large)', 210.00),
('Lemonade (Medium)', 170.00),
('Lemonade (Large)', 200.00),
('Sandwich Ham & Cheese', 220.00),
('Sandwich Salami & Mozzarella', 240.00);

UPDATE products p
JOIN product_price_updates u ON p.name = u.product_name
SET p.price = u.new_price
WHERE p.id > 0;

DROP TEMPORARY TABLE product_price_updates;
