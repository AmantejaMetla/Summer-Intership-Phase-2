# Images for the frontend (so it doesn’t look bad)

## Why the shop looks empty / “terrible”

The **ECONNREFUSED** errors in the terminal mean the **frontend can’t reach the backend**. The React app calls `/api/products` and `/api/products/categories`; if the gateway and product-service aren’t running, those requests fail and you get:

- No categories in the filter sidebar (or only “All”)
- No products in the grid
- Hero with no product carousel

**Fix:** Start your backend and MySQL, then reload the app.

1. **MySQL** – running (e.g. XAMPP or MySQL service).
2. **Gateway** – from project root:  
   `mvn -pl gateway -am -DskipTests spring-boot:run -Dspring-boot.run.profiles=mysql`
3. **Product-service** (if not started by `-am`):  
   `mvn -pl product-service -DskipTests spring-boot:run -Dspring-boot.run.profiles=mysql`

Once the API responds, products and categories load. Product images come from each product’s `image_url` (from the database / seed). Right now the seed uses placeholder URLs; you can replace them with your own images as below.

---

## What images you need

Use **one image per product**. The app has **24 products** from the coffee-shop dataset. Use these **exact file names** so the app can find them.

Put every file in:

**`frontend/public/images/`**

(e.g. `frontend/public/images/cappuccino-medium.jpg`)

---

## How to label the image files

| # | Product name | Label the image file (use this exact name) |
|---|----------------|--------------------------------------------|
| 1 | Cappuccino (Medium) | `cappuccino-medium.jpg` |
| 2 | Cappuccino (Large) | `cappuccino-large.jpg` |
| 3 | Latte (Medium) | `latte-medium.jpg` |
| 4 | Latte (Large) | `latte-large.jpg` |
| 5 | Flat White | `flat-white.jpg` |
| 6 | Caramel Macchiato (Medium) | `caramel-macchiato-medium.jpg` |
| 7 | Caramel Macchiato (Large) | `caramel-macchiato-large.jpg` |
| 8 | Espresso | `espresso.jpg` |
| 9 | Mocha (Medium) | `mocha-medium.jpg` |
| 10 | Mocha (Large) | `mocha-large.jpg` |
| 11 | White Mocha (Medium) | `white-mocha-medium.jpg` |
| 12 | White Mocha (Large) | `white-mocha-large.jpg` |
| 13 | Hot Chocolate (Medium) | `hot-chocolate-medium.jpg` |
| 14 | Hot Chocolate (Large) | `hot-chocolate-large.jpg` |
| 15 | Cold Coffee (Medium) | `cold-coffee-medium.jpg` |
| 16 | Cold Coffee (Large) | `cold-coffee-large.jpg` |
| 17 | Cold Mocha (Medium) | `cold-mocha-medium.jpg` |
| 18 | Cold Mocha (Large) | `cold-mocha-large.jpg` |
| 19 | Iced Tea (Medium) | `iced-tea-medium.jpg` |
| 20 | Iced Tea (Large) | `iced-tea-large.jpg` |
| 21 | Lemonade (Medium) | `lemonade-medium.jpg` |
| 22 | Lemonade (Large) | `lemonade-large.jpg` |
| 23 | Sandwich Ham & Cheese | `sandwich-ham-cheese.jpg` |
| 24 | Sandwich Salami & Mozzarella | `sandwich-salami-mozzarella.jpg` |

You can use **.jpg**, **.jpeg**, or **.png**. If you use PNG, rename the file in the table (e.g. `cappuccino-medium.png`) and the seed must use the same extension (see below).

---

## After you add the images

The product seed is already set to use these paths (e.g. `/images/cappuccino-medium.jpg`). As long as the files are in **`frontend/public/images/`** with the names above, the frontend will show them when the backend is running and returning products.

- **New setup:** Run the DB init scripts (e.g. `06-seed-products-from-dataset.sql`) so products get these image paths.
- **Already have products in MySQL?** If you seeded earlier with placeholder URLs, either clear the `products` table and run the seed again, or update each row’s `image_url` in MySQL to the matching `/images/...` path from the table above.

If you use different file names, update **`product-service/src/main/resources/product-seed.json`** (or the `image_url` column in the `products` table) so each product’s `imageUrl` matches the file name in `frontend/public/images/`.

---

## Quick checklist

1. Start **MySQL** and run init scripts (e.g. `01`, `06`) if the DB is empty.
2. Start **gateway** and **product-service** (with profile `mysql`).
3. Reload the frontend – products and categories should appear.
4. (Optional) Add your images to **`frontend/public/images/`** with the **exact labels** from the table above so the shop and hero look good.
