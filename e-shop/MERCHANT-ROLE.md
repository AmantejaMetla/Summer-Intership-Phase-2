# Merchant Role and Features

The e-shop supports three roles: **customer**, **admin**, and **merchant**. This document describes the merchant role and how to use it.

## Roles

- **customer** – Default for new registrations. Browse, cart, place orders, pay.
- **admin** – Can assign roles, manage any product (update/delete), update any product’s inventory, and see merchant sales.
- **merchant** – Can manage their own products and see orders that contain their products.

## Adding the merchant role (database)

- If you use the full init script **`init-db/01-single-database-all-tables.sql`**, the `merchant` role is already inserted.
- If your database already existed, run **`init-db/04-alter-products-seller-id-and-merchant-role.sql`** to add the `merchant` role and the `seller_id` column on `products`.

## Assigning the merchant role to a user

1. List roles: `GET /api/auth/roles` (no auth). Note the `id` for `merchant` (e.g. `3`).
2. Assign role: `POST /api/auth/users/{userId}/roles` with body `{"roleId": 3}`. Use an admin or any authenticated user who can call this endpoint.

A user can have multiple roles (e.g. both `customer` and `merchant`).

## What the merchant can do

### Products

- **Create product** – `POST /api/products` (with auth). The product’s `seller_id` is set to the logged-in user.
- **List my products** – `GET /api/products/my` (with auth). Returns only products where `seller_id` = your user id.
- **Update product** – `PUT /api/products/{id}` (with auth). Allowed only if you own the product or you are admin.
- **Delete product** – `DELETE /api/products/{id}` (with auth). Allowed only if you own the product or you are admin.

### Orders (sales view)

- **Merchant sales** – `GET /api/orders/merchant/sales` (with auth). Returns orders that contain at least one item whose product is sold by you. Requires `merchant` or `admin` role (403 otherwise).

### Inventory

- **Update stock** – `PUT /api/inventory/{productId}` with body `{"quantity": 50}` (with auth). Allowed only if you are the product’s seller or admin. Otherwise 403.

## How roles are passed

- On login/register/refresh, the auth service includes **roles** in the JWT.
- The API gateway reads the JWT and forwards **X-User-Id** and **X-Roles** (comma-separated, e.g. `customer,merchant`) to downstream services.
- Product, order, and inventory services use these headers to enforce ownership and role checks.

## Quick test flow

1. Register or log in as a user. Assign that user the **merchant** role (see above).
2. Create a product with `POST /api/products` (you become the seller).
3. Call `GET /api/products/my` – you should see that product.
4. Update it with `PUT /api/products/{id}` and/or update stock with `PUT /api/inventory/{productId}`.
5. As another user (customer), place an order that includes that product.
6. As the merchant, call `GET /api/orders/merchant/sales` – you should see that order.

Postman: use folder **Merchant (full flow)** (steps 1–7) or individual requests under Auth / Product / Order / Inventory.

---

## Merchant test checklist

Use this to verify the full merchant flow end-to-end.

1. **Database**
   - [ ] Ran `init-db/01-single-database-all-tables.sql` (new DB) or `04-alter-products-seller-id-and-merchant-role.sql` (existing DB).

2. **Assign merchant role**
   - [ ] Login or register (e.g. `merchant@test.com`).
   - [ ] Call **GET /api/auth/roles** and note `merchant` role id (usually `3`).
   - [ ] Call **POST /api/auth/users/{userId}/roles** with body `{"roleId": 3}` (use your `userId` from login/register response).

3. **Merchant product flow**
   - [ ] **POST /api/products** (with auth) – create a product (you become seller).
   - [ ] **GET /api/products/my** – list shows your product(s).
   - [ ] **PUT /api/products/{id}** – update name/price/stock (only your products).
   - [ ] **PUT /api/inventory/{productId}** with `{"quantity": 50}` – update stock (only your products).

4. **Merchant sales**
   - [ ] As a **customer** (another user), place an order that includes your product.
   - [ ] As the **merchant**, call **GET /api/orders/merchant/sales** – that order appears.

5. **test-api.html**
   - [ ] Open `test-api.html`, login → see **User id** and **Roles**.
   - [ ] **List roles** → **Assign role** (user id = your id, role id = 3).
   - [ ] **Create product** → **List my products** → **Merchant sales** → **Update stock** (use product id from list).
