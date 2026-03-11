-- =============================================================================
-- Repair products in existing MySQL data:
-- - restore curated INR prices
-- - restore local frontend image URLs (/images/*.jpg)
-- Safe-update compatible (uses JOIN on product name).
-- =============================================================================

USE eshop;

SET @OLD_SQL_SAFE_UPDATES := @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

DROP TEMPORARY TABLE IF EXISTS product_catalog_updates;
DROP TEMPORARY TABLE IF EXISTS product_catalog_updates_by_id;

CREATE TEMPORARY TABLE product_catalog_updates (
  product_name VARCHAR(255) PRIMARY KEY,
  new_price DECIMAL(10,2) NOT NULL,
  new_image_url VARCHAR(512) NOT NULL
);

INSERT INTO product_catalog_updates (product_name, new_price, new_image_url) VALUES
('Cappuccino (Medium)', 255.00, '/images/cappuccino-medium.jpg'),
('Cappuccino (Large)', 285.00, '/images/cappuccino-large.jpg'),
('Latte (Medium)', 250.00, '/images/latte-medium.jpg'),
('Latte (Large)', 280.00, '/images/latte-large.jpg'),
('Flat White', 245.00, '/images/flat-white.jpg'),
('Caramel Macchiato (Medium)', 275.00, '/images/caramel-macchiato-medium.jpg'),
('Caramel Macchiato (Large)', 305.00, '/images/caramel-macchiato-large.jpg'),
('Espresso', 190.00, '/images/espresso.jpg'),
('Mocha (Medium)', 270.00, '/images/mocha-medium.jpg'),
('Mocha (Large)', 300.00, '/images/mocha-large.jpg'),
('White Mocha (Medium)', 275.00, '/images/white-mocha-medium.jpg'),
('White Mocha (Large)', 305.00, '/images/white-mocha-large.jpg'),
('Hot Chocolate (Medium)', 250.00, '/images/hot-chocolate-medium.jpg'),
('Hot Chocolate (Large)', 280.00, '/images/hot-chocolate-large.jpg'),
('Cold Coffee (Medium)', 235.00, '/images/cold-coffee-medium.jpg'),
('Cold Coffee (Large)', 265.00, '/images/cold-coffee-large.jpg'),
('Cold Mocha (Medium)', 260.00, '/images/cold-mocha-medium.jpg'),
('Cold Mocha (Large)', 290.00, '/images/cold-mocha-large.jpg'),
('Iced Tea (Medium)', 180.00, '/images/iced-tea-medium.jpg'),
('Iced Tea (Large)', 210.00, '/images/iced-tea-large.jpg'),
('Lemonade (Medium)', 170.00, '/images/lemonade-medium.jpg'),
('Lemonade (Large)', 200.00, '/images/lemonade-large.jpg'),
('Sandwich Ham & Cheese', 220.00, '/images/sandwich-ham-cheese.jpg'),
('Sandwich Salami & Mozzarella', 240.00, '/images/sandwich-salami-mozzarella.jpg');

CREATE TEMPORARY TABLE product_catalog_updates_by_id (
  product_id BIGINT PRIMARY KEY,
  new_price DECIMAL(10,2) NOT NULL,
  new_image_url VARCHAR(512) NOT NULL
);

INSERT INTO product_catalog_updates_by_id (product_id, new_price, new_image_url)
SELECT p.id, u.new_price, u.new_image_url
FROM products p
JOIN product_catalog_updates u ON p.name = u.product_name
WHERE p.id IS NOT NULL;

UPDATE products p
JOIN product_catalog_updates_by_id u ON p.id = u.product_id
SET p.price = u.new_price,
    p.image_url = u.new_image_url
WHERE p.id = u.product_id;

DROP TEMPORARY TABLE product_catalog_updates;
DROP TEMPORARY TABLE product_catalog_updates_by_id;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;
