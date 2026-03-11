# All services, API Gateway, MongoDB & Postman

Summary of what’s implemented, how the gateway connects everything, and how to show it (including MongoDB and Postman).

---

## 1. All services – implemented and working

| Service          | Port | Database   | Purpose                          |
|------------------|------|------------|----------------------------------|
| **eureka-server**| 8761 | —          | Service registry (discovery)     |
| **auth-service** | 9081 | MySQL/H2   | Register, login, JWT, refresh    |
| **user-service** | 9082 | MySQL/H2   | User profile (get/update)       |
| **product-service** | 9083 | MySQL/H2 | Products (list, get by id)       |
| **cart-service** | 9087 | **MongoDB**| Cart (get, add items, clear)     |
| **order-service**| 9084 | MySQL/H2   | Orders                           |
| **inventory-service** | 9085 | MySQL/H2 | Stock by product                 |
| **payment-service** | 9086 | MySQL/H2 | Payments                         |
| **api-gateway**  | 9090 | —          | Single entry point, routes, JWT |

Yes – all of the above are implemented. Clients talk only to the gateway (port 9090); the gateway routes to the right service using Eureka.

**Diagram coverage:** Categories (product-service), order line items (order-service), and roles (auth-service) are implemented so the data model is fully covered: **Categories** – CRUD at `/api/products/categories`; products can have `categoryId` and `stockQuantity`. **Order items** – `GET/POST /api/orders/{orderId}/items`. **Roles** – `GET /api/auth/roles`, `POST /api/auth/users/{userId}/roles` (assign role); register auto-assigns **customer**; login/register/refresh responses include a **roles** array.

---

## 2. How the API Gateway connects all of them

- Every request goes to **http://localhost:9090** (or your gateway URL).
- The gateway uses **path** to decide which service to call.
- It looks up the service in **Eureka** (e.g. `lb://cart-service`) and forwards the request there.
- The gateway also checks **JWT** for protected routes and adds **X-User-Id** for user-scoped APIs.

**Path → service mapping:**

| Path prefix     | Service           | Example request              |
|-----------------|-------------------|------------------------------|
| `/api/auth/**`  | auth-service      | POST /api/auth/login         |
| `/api/users/**` | user-service      | GET /api/users/me            |
| `/api/products/**` | product-service | GET /api/products            |
| `/api/cart/**`  | **cart-service**  | GET /api/cart, POST /api/cart/items |
| `/api/orders/**`| order-service     | GET /api/orders/1            |
| `/api/inventory/**` | inventory-service | GET /api/inventory/products/1 |
| `/api/payments/**` | payment-service | GET /api/payments/1          |

Static/gateway-only: `/`, `/actuator/health`, `/actuator/info`, `/test` are handled by the gateway itself (no backend service).

---

## 3. How to show that the gateway connects everything

1. **Single URL**  
   All calls use one base: `http://localhost:9090`. No need to remember per-service ports (9081, 9082, …).

2. **Eureka dashboard**  
   - Open **http://localhost:8761**.  
   - You should see all apps: AUTH-SERVICE, USER-SERVICE, PRODUCT-SERVICE, CART-SERVICE, ORDER-SERVICE, INVENTORY-SERVICE, PAYMENT-SERVICE, API-GATEWAY.  
   - This shows that the gateway (and other services) discover each other via Eureka.

3. **Postman through the gateway**  
   - Set **baseUrl** to `http://localhost:9090`.  
   - Run **Auth → Login**, then **User → Get my profile**, **Product → List products**, **Cart → Get my cart**, etc.  
   - All go to 9090; the gateway routes each path to the correct service. One entry point = gateway connects all of them.

4. **Optional: call a service port directly**  
   For comparison, call e.g. `http://localhost:9087/api/cart` with a valid token (and `X-User-Id` if required). Same result as via `http://localhost:9090/api/cart` – proves the gateway is just routing to cart-service.

---

## 4. MongoDB integration (Cart service)

- **Only Cart service** uses MongoDB; the rest use MySQL (or H2 by default).
- **Connection:** `localhost:27017`, database **`cart_db`**, collection **`carts`** (see cart-service `application.yml` and `@Document(collection = "carts")`).
- **Postman:** Cart requests go to the gateway (`/api/cart`, `/api/cart/items`). The gateway forwards to cart-service; cart-service reads/writes MongoDB. So “Postman → Gateway → Cart service → MongoDB” is the full flow.

---

## 5. Postman integration

- **One base URL:** Collection variable **baseUrl** = `http://localhost:9090`. Every request in the collection uses `{{baseUrl}}/api/...`.
- **Auth:** **Auth → Login** (or Register) returns `accessToken`; the collection script saves it. Protected requests use **Authorization: Bearer {{accessToken}}**.
- **Flow:** Gateway Health → Auth Login → User, Product, Cart, Order, Inventory, Payment. All go through the gateway; no need to touch service ports. That’s the “Postman integration” with the gateway and all services.

---

For step-by-step run order, import, and test (including **MongoDB verification in Compass**), see **postman/README.md**.
