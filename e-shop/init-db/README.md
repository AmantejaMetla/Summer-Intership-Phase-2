# MySQL setup for demo laptop

Run these SQL files **on the demo laptop** after MySQL is installed and running. Order matters.

---

## Option A: Full schema from diagram (one database, one script)

Use this when you want **all tables from the data model diagram** in a single database (e.g. for reference or a single-DB setup).

**File:** `00-full-schema-from-diagram.sql`

- Run this **once** in MySQL (Workbench, command line, or any SQL client).
- Creates database **`eshop`** and all 10 tables: `users`, `roles`, `user_roles`, `categories`, `products`, `carts`, `cart_items`, `orders`, `order_items`, `payments`.
- Includes foreign keys and optional seed data for roles (`customer`, `admin`).

**Note:** The microservices in this project use **separate databases** (Option B). Option A is for the exact diagram schema in one place; Cart in the running app uses **MongoDB**, not MySQL.

---

## Option B: One database for all microservices (recommended if you need a single DB)

Use this when you are **required to use only one database**. All six services (auth, user, product, order, inventory, payment) connect to the same database.

**File:** `01-single-database-all-tables.sql`

- Run this **once** in MySQL (Workbench, command line, or any SQL client).
- Creates database **`eshop`** and **all tables** used by auth-, user-, product-, order-, inventory-, and payment-service (user_credentials, refresh_tokens, roles, user_roles, user_profiles, categories, products, orders, order_items, stock, payments).
- Then start each of those six services with profile **`mysql`**; they are already configured to use `eshop`.

**Cart** still uses **MongoDB** (no MySQL).

**Seed products from dataset (optional):** After running `01-single-database-all-tables.sql`, run **`06-seed-products-from-dataset.sql`** to insert the 24 coffee-shop products and 3 categories (Hot Drinks, Cold Drinks, Snacks) from the Kaggle dataset. Alternatively, product-service will auto-load the same data from `product-seed.json` on first start if the `products` table is empty.

**Seed orders from dataset (optional):** After 06, run **`07-seed-orders-from-dataset.sql`** to insert orders and order_items from **orders.csv** (and a seed user id=1 if missing). Generate 07 by running `node tools/generate-orders-seed.js` from the project root if you need to regenerate it.

---

## Option C: Microservices (separate databases per service)

Use this only if you want **one database per service** (auth_db, user_db, etc.). The project is currently configured for **one database (eshop)**; if you need separate DBs again, change each service’s `application-mysql.yml` back to its `*_db` URL and use the scripts below.

### 1. Create databases (run first)

**File:** `01-create-databases.sql`

- Run once. Creates: `auth_db`, `user_db`, `product_db`, `order_db`, `inventory_db`, `payment_db`.

### 2. Create tables per service (optional but recommended)

Run each file in order. Each file starts with `USE <database>;` so you can run it in MySQL against that database:

| File | Database | Service |
|------|----------|---------|
| `02-tables-auth_db.sql` | auth_db | auth-service |
| `02-tables-user_db.sql` | user_db | user-service |
| `02-tables-product_db.sql` | product_db | product-service (includes `categories`) |
| `02-tables-order_db.sql` | order_db | order-service (includes `order_items`) |
| `02-tables-inventory_db.sql` | inventory_db | inventory-service |
| `02-tables-payment_db.sql` | payment_db | payment-service |

**How to run:** Open each file, copy its contents, and execute in MySQL. Run `01-create-databases.sql` first, then each `02-tables-*.sql`.

**Alternative:** You can skip the table scripts. With `spring.jpa.hibernate.ddl-auto: update`, each service will create/update its tables when it first connects to MySQL (with `mysql` profile).

---

## Summary

| Goal | Use |
|------|-----|
| **One database for all services** (required) | Run **`01-single-database-all-tables.sql`** once. Then start auth, user, product, order, inventory, payment with profile **mysql**. |
| Exact diagram: 10 tables in one DB (reference) | Run **`00-full-schema-from-diagram.sql`** only. (Not used by the running app.) |
| Separate DB per service | Run **`01-create-databases.sql`**, then **`02-tables-*.sql`** for each DB; and reconfigure each service’s `application-mysql.yml` to use its own `*_db`. |

## Default credentials

Configs assume MySQL user `root` and password `root`. Change in each service’s `application.yml` (or via env vars) if your demo laptop uses different credentials.
