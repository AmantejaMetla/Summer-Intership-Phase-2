# Full guide: Setup → Import → Test

From zero to “all testing works” in one place. Use this to run the system and test (or show your mentor) via Postman.

---

## Part 1: Setup (before testing)

### What you need

- **Java 21** and **Maven** (or project Maven in `tools/` — see SETUP.md).
- **MongoDB** running (for Cart service). Start it (e.g. run `mongod` or use MongoDB as a service). Optional: MySQL only if you use the `mysql` profile (see SETUP.md).

### Run the services (in order)

Use **one terminal per service**. From the **project root** each time.

**Step 1 – Eureka (first)**  
```bash
cd eureka-server
mvn spring-boot:run
```  
Wait until it’s up. Open http://localhost:8761 to confirm.

**Step 2 – Microservices** (each in a new terminal, from project root)  
```bash
cd auth-service      && mvn spring-boot:run
cd user-service      && mvn spring-boot:run
cd product-service   && mvn spring-boot:run
cd cart-service      && mvn spring-boot:run
cd order-service     && mvn spring-boot:run
cd inventory-service && mvn spring-boot:run
cd payment-service   && mvn spring-boot:run
```

**Step 3 – API Gateway (last)**  
```bash
cd api-gateway
mvn spring-boot:run
```

**Check:**  
- Eureka: http://localhost:8761 — all services (e.g. AUTH-SERVICE, USER-SERVICE, API-GATEWAY) should appear.  
- Gateway: http://localhost:9090/actuator/health — should return `{"status":"UP"}`.  
All API testing goes through **http://localhost:9090** (or http://192.168.1.3:9090 if on another machine).

---

## Part 2: Import Postman collection

1. Open **Postman**.
2. Click **Import** (or File → Import).
3. Choose **E-Shop-API.postman_collection.json** from the project folder:  
   `E-Shopping Case Study/postman/E-Shop-API.postman_collection.json`
4. Click **Import**. The collection **E-Shop API** appears in the sidebar.
5. (Optional) If your gateway is not on this PC: click the collection → **Variables** → set **baseUrl** to e.g. `http://192.168.1.3:9090`.

---

## Part 3: Test (what to do in Postman)

Follow this order. After **Login** (or **Register**), the token is saved automatically for other requests.

| # | What to do | In Postman | What you should see |
|---|------------|------------|---------------------|
| 1 | Check gateway | **Gateway → Health** → Send | `{"status":"UP"}` |
| 2 | Check gateway | **Gateway → Info** → Send | Gateway info JSON |
| 3 | Get token | **Auth → Login** → Send (body: email + password) | 200, body has `accessToken` and `refreshToken` (token is saved for rest of collection) |
| 4 | Test User service | **User → Get my profile** → Send | 200, your profile JSON (or new profile if first time) |
| 5 | Test Product service | **Product → List products** → Send | 200, array of products (can be empty) |
| 6 | Test Cart service | **Cart → Get my cart** → Send | 200, cart JSON (empty at first) |
| 7 | (Optional) Cart | **Cart → Add to cart** → Send (edit body if needed) | 200, cart with item |
| 8 | (Optional) User | **User → Update my profile** → Send | 200, updated profile |
| 9 | (Optional) **Roles** | **Auth → List roles** → Send | 200, list of roles (e.g. customer, admin). Login/Register response now includes **roles** array. |
| 10 | (Optional) **Categories** | **Product → List categories** → Send | 200, list of categories. **Create category** to add one; then **Create product** with `categoryId`. **List products by categoryId** to filter. |
| 11 | (Optional) **Order items** | **Order → Create order** (body: totalAmount, status) → Send; note the returned order **id**. Then **Order → Get order items** (replace id in URL), **Order → Add order item** (replace order id in URL; body: productId, quantity, price). | 200, order items list or new item. |
| 12 | (Optional) Order / Inventory / Payment | Use **Order → Get order by id**, **Inventory → Get stock by product id**, **Payment → Get payment by id** as needed | 200 if resource exists, or 404 |

If steps 1–6 work, **all main testing is done**: gateway, auth, user, product, and cart are verified. Order, Inventory, and Payment work the same way (same base URL and token where required).

---

## Part 4: MongoDB – final steps (verify Cart in MongoDB)

Cart is the only service that uses **MongoDB**. After you run **Cart → Get my cart** or **Cart → Add to cart** in Postman, you can confirm data in MongoDB.

### 1. Ensure MongoDB and cart-service are running

- Start **MongoDB** (e.g. `mongod` or MongoDB as a Windows service).
- Start **cart-service**: from project root, `cd cart-service` then `mvn spring-boot:run`.

### 2. Create cart data via Postman

- **Auth → Login** (so `accessToken` is set).
- **Cart → Get my cart** → Send (may be empty).
- **Cart → Add to cart** → Send (body: `productId`, `productName`, `quantity`, `unitPrice`).
- **Cart → Get my cart** again → you should see the item.

### 3. Verify in MongoDB Compass

1. Open **MongoDB Compass**.
2. Connect to **`mongodb://localhost:27017`**.
3. In the left sidebar, open database **`cart_db`** (Cart service uses this database).
4. Open the collection **`carts`**.
5. You should see at least one document: your user’s cart (e.g. `userId`, `items` array). After “Add to cart”, the document will include the new item.

This confirms: **Postman → Gateway (9090) → Cart service → MongoDB (cart_db.carts)**.

---

## Part 5: Testing the new features (Roles, Categories, Order items)

Use this section **on this computer** (or the demo laptop) once the main flow (Parts 1–4) works. All requests go through the gateway with your saved token.

### Prerequisites

- Services are running (Eureka, Gateway, auth, user, product, order, inventory, payment, cart if using MongoDB).
- Postman collection imported; **Auth → Login** (or Register) already run so **accessToken** is set.

---

### A. Roles

1. **Auth → Login** (or Register) → Send.  
   - In the response body, confirm there is a **`roles`** array (e.g. `["customer"]`). New users get the **customer** role automatically.
2. **Auth → List roles** → Send.  
   - You should get **200** and a list of roles, e.g. `[{"id":1,"roleName":"customer"},{"id":2,"roleName":"admin"}]`.
3. (Optional) **Auth → Assign role to user**  
   - Your user id is the one used in the JWT (e.g. 1 for first user). Edit the URL: replace `1` with your user id.  
   - Body: `{"roleId": 2}` to assign **admin**. Send.  
   - **Auth → Login** again and check the response: **roles** may now include both.

---

### B. Categories and products

1. **Product → List categories** → Send.  
   - First time: **200** with an empty array `[]` (or existing categories if you added some).
2. **Product → Create category** → Send.  
   - Body is pre-filled, e.g. `{"categoryName": "Electronics"}`. Change the name if you like. You should get **200** and the created category with an **id**.
3. **Product → Create product** → Send.  
   - Body example: `{"name": "Widget", "description": "A widget", "price": 19.99, "category": "Electronics", "categoryId": 1, "stockQuantity": 100}`.  
   - Use the **id** from step 2 for **categoryId** (e.g. 1). You should get **200** and the created product.
4. **Product → List products by categoryId** → Send.  
   - URL is `.../api/products?categoryId=1`. Use the same category id. You should get **200** and a list containing your product.

---

### C. Order and order items

1. **Order → Create order** → Send.  
   - Body: `{"totalAmount": 0, "status": "PENDING"}`. You should get **200** and an order with an **id** (e.g. 1).
2. Note the order **id** from the response (e.g. `1`).
3. **Order → Get order items** → Open the request and change the URL: replace the order id with yours (e.g. `.../api/orders/1/items`). Send.  
   - You should get **200** and an empty array `[]`.
4. **Order → Add order item** → Change the URL to use the same order id (e.g. `.../api/orders/1/items`).  
   - Body: `{"productId": 1, "quantity": 2, "price": 9.99}`. Use a **productId** that exists (e.g. from **Create product**). Send.  
   - You should get **200** and the created order item.
5. **Order → Get order items** → Send again (same order id).  
   - You should get **200** and a list with one item.

---

### Quick checklist

| Feature    | Request(s) | Expected |
|-----------|------------|----------|
| Roles in login | Auth → Login | Response has `roles` array |
| List roles     | Auth → List roles | 200, list of roles |
| Categories      | Product → List categories, Create category | 200, category with id |
| Product + category | Product → Create product (with categoryId), List products by categoryId | 200, product and filtered list |
| Order items    | Order → Create order → Get order items → Add order item → Get order items | 200, empty list then list with item |

If any step fails, check that the gateway (9090) and the right service are up, and that **Authorization: Bearer {{accessToken}}** is present (token from Login/Register).

---

## Summary for your mentor

- **Setup:** Run Eureka → all 7 microservices → API Gateway (commands in Part 1). MongoDB must be running for Cart.
- **Import:** Postman → Import → `E-Shop-API.postman_collection.json` (Part 2).
- **Test:** Gateway Health → Auth Login → User Get my profile → Product List → Cart Get my cart (Part 3). One token from Login is used for all protected requests.
- **MongoDB:** Cart service uses database `cart_db`, collection `carts`; verify in Compass (Part 4).
- **New features:** Roles, Categories, Order items — step-by-step testing in **Part 5** (same doc).

For “all services, how the gateway connects them, and how to show it”, see **SERVICES-AND-GATEWAY.md** in the project root.  
For MySQL, corporate laptop, or transfer setup, see **SETUP.md** and **RUN-ON-DEMO-LAPTOP.md** in the project root.
