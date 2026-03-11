-- =============================================================================
-- Force-refresh product prices + image URLs without temp tables.
-- Use this when Workbench temp-table/session issues block script 11.
-- =============================================================================

USE eshop;

SET @OLD_SQL_SAFE_UPDATES := @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

UPDATE products
SET
  price = CASE name
    WHEN 'Cappuccino (Medium)' THEN 255.00
    WHEN 'Cappuccino (Large)' THEN 285.00
    WHEN 'Latte (Medium)' THEN 250.00
    WHEN 'Latte (Large)' THEN 280.00
    WHEN 'Flat White' THEN 245.00
    WHEN 'Caramel Macchiato (Medium)' THEN 275.00
    WHEN 'Caramel Macchiato (Large)' THEN 305.00
    WHEN 'Espresso' THEN 190.00
    WHEN 'Mocha (Medium)' THEN 270.00
    WHEN 'Mocha (Large)' THEN 300.00
    WHEN 'White Mocha (Medium)' THEN 275.00
    WHEN 'White Mocha (Large)' THEN 305.00
    WHEN 'Hot Chocolate (Medium)' THEN 250.00
    WHEN 'Hot Chocolate (Large)' THEN 280.00
    WHEN 'Cold Coffee (Medium)' THEN 235.00
    WHEN 'Cold Coffee (Large)' THEN 265.00
    WHEN 'Cold Mocha (Medium)' THEN 260.00
    WHEN 'Cold Mocha (Large)' THEN 290.00
    WHEN 'Iced Tea (Medium)' THEN 180.00
    WHEN 'Iced Tea (Large)' THEN 210.00
    WHEN 'Lemonade (Medium)' THEN 170.00
    WHEN 'Lemonade (Large)' THEN 200.00
    WHEN 'Sandwich Ham & Cheese' THEN 220.00
    WHEN 'Sandwich Salami & Mozzarella' THEN 240.00
    ELSE price
  END,
  image_url = CASE name
    WHEN 'Cappuccino (Medium)' THEN '/images/cappuccino-medium.jpg'
    WHEN 'Cappuccino (Large)' THEN '/images/cappuccino-large.jpg'
    WHEN 'Latte (Medium)' THEN '/images/latte-medium.jpg'
    WHEN 'Latte (Large)' THEN '/images/latte-large.jpg'
    WHEN 'Flat White' THEN '/images/flat-white.jpg'
    WHEN 'Caramel Macchiato (Medium)' THEN '/images/caramel-macchiato-medium.jpg'
    WHEN 'Caramel Macchiato (Large)' THEN '/images/caramel-macchiato-large.jpg'
    WHEN 'Espresso' THEN '/images/espresso.jpg'
    WHEN 'Mocha (Medium)' THEN '/images/mocha-medium.jpg'
    WHEN 'Mocha (Large)' THEN '/images/mocha-large.jpg'
    WHEN 'White Mocha (Medium)' THEN '/images/white-mocha-medium.jpg'
    WHEN 'White Mocha (Large)' THEN '/images/white-mocha-large.jpg'
    WHEN 'Hot Chocolate (Medium)' THEN '/images/hot-chocolate-medium.jpg'
    WHEN 'Hot Chocolate (Large)' THEN '/images/hot-chocolate-large.jpg'
    WHEN 'Cold Coffee (Medium)' THEN '/images/cold-coffee-medium.jpg'
    WHEN 'Cold Coffee (Large)' THEN '/images/cold-coffee-large.jpg'
    WHEN 'Cold Mocha (Medium)' THEN '/images/cold-mocha-medium.jpg'
    WHEN 'Cold Mocha (Large)' THEN '/images/cold-mocha-large.jpg'
    WHEN 'Iced Tea (Medium)' THEN '/images/iced-tea-medium.jpg'
    WHEN 'Iced Tea (Large)' THEN '/images/iced-tea-large.jpg'
    WHEN 'Lemonade (Medium)' THEN '/images/lemonade-medium.jpg'
    WHEN 'Lemonade (Large)' THEN '/images/lemonade-large.jpg'
    WHEN 'Sandwich Ham & Cheese' THEN '/images/sandwich-ham-cheese.jpg'
    WHEN 'Sandwich Salami & Mozzarella' THEN '/images/sandwich-salami-mozzarella.jpg'
    ELSE image_url
  END
WHERE name IN (
  'Cappuccino (Medium)', 'Cappuccino (Large)', 'Latte (Medium)', 'Latte (Large)', 'Flat White',
  'Caramel Macchiato (Medium)', 'Caramel Macchiato (Large)', 'Espresso', 'Mocha (Medium)', 'Mocha (Large)',
  'White Mocha (Medium)', 'White Mocha (Large)', 'Hot Chocolate (Medium)', 'Hot Chocolate (Large)',
  'Cold Coffee (Medium)', 'Cold Coffee (Large)', 'Cold Mocha (Medium)', 'Cold Mocha (Large)',
  'Iced Tea (Medium)', 'Iced Tea (Large)', 'Lemonade (Medium)', 'Lemonade (Large)',
  'Sandwich Ham & Cheese', 'Sandwich Salami & Mozzarella'
);

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;
