-- =============================================================================
-- Seed products and categories from the Kaggle dataset (items.csv).
-- Run this AFTER 01-single-database-all-tables.sql (so eshop database and
-- categories/products tables exist). Run once to insert the 24 coffee-shop
-- products and 3 categories into the SQL database.
-- =============================================================================

USE eshop;

-- Ensure categories exist (ignore if already present)
INSERT IGNORE INTO categories (category_name) VALUES ('Hot Drinks'), ('Cold Drinks'), ('Snacks');

-- Insert products (one INSERT per row to avoid long-statement / syntax issues)
INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Cappuccino (Medium)', 'Hot Drinks, Medium', 255.00, 'Hot Drinks', (SELECT id FROM categories WHERE category_name = 'Hot Drinks' LIMIT 1), 50, '/images/cappuccino-medium.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Cappuccino (Large)', 'Hot Drinks, Large', 285.00, 'Hot Drinks', (SELECT id FROM categories WHERE category_name = 'Hot Drinks' LIMIT 1), 50, '/images/cappuccino-large.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Latte (Medium)', 'Hot Drinks, Medium', 250.00, 'Hot Drinks', (SELECT id FROM categories WHERE category_name = 'Hot Drinks' LIMIT 1), 50, '/images/latte-medium.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Latte (Large)', 'Hot Drinks, Large', 280.00, 'Hot Drinks', (SELECT id FROM categories WHERE category_name = 'Hot Drinks' LIMIT 1), 50, '/images/latte-large.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Flat White', 'Hot Drinks', 245.00, 'Hot Drinks', (SELECT id FROM categories WHERE category_name = 'Hot Drinks' LIMIT 1), 50, '/images/flat-white.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Caramel Macchiato (Medium)', 'Hot Drinks, Medium', 275.00, 'Hot Drinks', (SELECT id FROM categories WHERE category_name = 'Hot Drinks' LIMIT 1), 50, '/images/caramel-macchiato-medium.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Caramel Macchiato (Large)', 'Hot Drinks, Large', 305.00, 'Hot Drinks', (SELECT id FROM categories WHERE category_name = 'Hot Drinks' LIMIT 1), 50, '/images/caramel-macchiato-large.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Espresso', 'Hot Drinks', 190.00, 'Hot Drinks', (SELECT id FROM categories WHERE category_name = 'Hot Drinks' LIMIT 1), 50, '/images/espresso.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Mocha (Medium)', 'Hot Drinks, Medium', 270.00, 'Hot Drinks', (SELECT id FROM categories WHERE category_name = 'Hot Drinks' LIMIT 1), 50, '/images/mocha-medium.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Mocha (Large)', 'Hot Drinks, Large', 300.00, 'Hot Drinks', (SELECT id FROM categories WHERE category_name = 'Hot Drinks' LIMIT 1), 50, '/images/mocha-large.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('White Mocha (Medium)', 'Hot Drinks, Medium', 275.00, 'Hot Drinks', (SELECT id FROM categories WHERE category_name = 'Hot Drinks' LIMIT 1), 50, '/images/white-mocha-medium.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('White Mocha (Large)', 'Hot Drinks, Large', 305.00, 'Hot Drinks', (SELECT id FROM categories WHERE category_name = 'Hot Drinks' LIMIT 1), 50, '/images/white-mocha-large.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Hot Chocolate (Medium)', 'Hot Drinks, Medium', 250.00, 'Hot Drinks', (SELECT id FROM categories WHERE category_name = 'Hot Drinks' LIMIT 1), 50, '/images/hot-chocolate-medium.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Hot Chocolate (Large)', 'Hot Drinks, Large', 280.00, 'Hot Drinks', (SELECT id FROM categories WHERE category_name = 'Hot Drinks' LIMIT 1), 50, '/images/hot-chocolate-large.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Cold Coffee (Medium)', 'Cold Drinks, Medium', 235.00, 'Cold Drinks', (SELECT id FROM categories WHERE category_name = 'Cold Drinks' LIMIT 1), 50, '/images/cold-coffee-medium.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Cold Coffee (Large)', 'Cold Drinks, Large', 265.00, 'Cold Drinks', (SELECT id FROM categories WHERE category_name = 'Cold Drinks' LIMIT 1), 50, '/images/cold-coffee-large.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Cold Mocha (Medium)', 'Cold Drinks, Medium', 260.00, 'Cold Drinks', (SELECT id FROM categories WHERE category_name = 'Cold Drinks' LIMIT 1), 50, '/images/cold-mocha-medium.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Cold Mocha (Large)', 'Cold Drinks, Large', 290.00, 'Cold Drinks', (SELECT id FROM categories WHERE category_name = 'Cold Drinks' LIMIT 1), 50, '/images/cold-mocha-large.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Iced Tea (Medium)', 'Cold Drinks, Medium', 180.00, 'Cold Drinks', (SELECT id FROM categories WHERE category_name = 'Cold Drinks' LIMIT 1), 50, '/images/iced-tea-medium.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Iced Tea (Large)', 'Cold Drinks, Large', 210.00, 'Cold Drinks', (SELECT id FROM categories WHERE category_name = 'Cold Drinks' LIMIT 1), 50, '/images/iced-tea-large.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Lemonade (Medium)', 'Cold Drinks, Medium', 170.00, 'Cold Drinks', (SELECT id FROM categories WHERE category_name = 'Cold Drinks' LIMIT 1), 50, '/images/lemonade-medium.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Lemonade (Large)', 'Cold Drinks, Large', 200.00, 'Cold Drinks', (SELECT id FROM categories WHERE category_name = 'Cold Drinks' LIMIT 1), 50, '/images/lemonade-large.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Sandwich Ham & Cheese', 'Snacks', 220.00, 'Snacks', (SELECT id FROM categories WHERE category_name = 'Snacks' LIMIT 1), 50, '/images/sandwich-ham-cheese.jpg');

INSERT INTO products (`name`, `description`, `price`, `category`, `category_id`, `stock_quantity`, `image_url`)
VALUES ('Sandwich Salami & Mozzarella', 'Snacks', 240.00, 'Snacks', (SELECT id FROM categories WHERE category_name = 'Snacks' LIMIT 1), 50, '/images/sandwich-salami-mozzarella.jpg');
