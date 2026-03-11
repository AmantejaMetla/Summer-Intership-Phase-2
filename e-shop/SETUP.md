# Setup Guide

Use this when you've unzipped the project on a new machine. You need: **Java 21**, **Maven**. **MongoDB** is needed only for the Cart service.

**Default:** All MySQL-backed services (auth, user, product, order, inventory, payment) use **H2 in-memory** by default — no MySQL required. Just run `mvn spring-boot:run` in each service folder. Data is lost when you stop the app.

**With MySQL (e.g. demo laptop):** Install MySQL, create the DBs (see below), then start those services with profile `mysql`:  
`mvn spring-boot:run -Dspring-boot.run.profiles=mysql`

---

## 1. MySQL (only when you want persistent data / demo laptop)

- Install and start MySQL.
- Create the databases (run once). From MySQL client or any SQL tool, run:
  - **File:** `init-db/01-create-databases.sql`
  - Optionally run the table scripts in `init-db/` (see `init-db/README.md`): `02-tables-auth_db.sql`, `02-tables-user_db.sql`, etc. If you skip them, JPA will create/update tables on first app start.
- Default in configs: user `root`, password `root`. Change in each service's `application.yml` or via env vars if different.

---

## 2. MongoDB

- Install and start MongoDB (default port 27017).
- No DB creation needed; Cart service uses database `cart_db` automatically.

---

## 3. Start services (in order)

Open a terminal per service (or reuse one: run a service, then open a new terminal for the next). From the **project root**:

**Step 1 – Eureka (must be first)**  
```bash
cd eureka-server
mvn spring-boot:run
```  
Wait until it’s up (e.g. http://localhost:8761).

**Step 2 – All microservices**  
In separate terminals. Default uses H2 (no MySQL). If you have MySQL and want persistent data, add `-Dspring-boot.run.profiles=mysql` to auth, user, product, order, inventory, payment.

```bash
cd auth-service     && mvn spring-boot:run
cd user-service     && mvn spring-boot:run
cd product-service  && mvn spring-boot:run
cd cart-service     && mvn spring-boot:run
cd order-service    && mvn spring-boot:run
cd inventory-service && mvn spring-boot:run
cd payment-service  && mvn spring-boot:run
```

**Step 3 – API Gateway (last)**  
```bash
cd api-gateway
mvn spring-boot:run
```

---

## 4. Use the app

- **Eureka dashboard:** http://localhost:8761  
- **All API calls go through the gateway:** http://localhost:9090  

**Quick test:**  
- Register: `POST http://localhost:9090/api/auth/register`  
  Body (JSON): `{"email":"test@example.com","password":"password123"}`  
- Then: `GET http://localhost:9090/api/users/me` with header: `Authorization: Bearer <accessToken from register>`  

---

## 5. If something fails

- **MySQL (Communications link failure):** By default the app uses H2 — you should not see this unless you passed `-Dspring-boot.run.profiles=mysql`. If using the mysql profile, ensure MySQL is running and the DBs exist (section 1).
- **MongoDB:** Ensure it’s running on 27017 (or set `MONGODB_HOST`/`MONGODB_PORT` for cart-service).
- **Eureka:** Other services must start *after* Eureka; they will retry registration.
- **Gateway:** Start it after the other services so routes resolve correctly.
