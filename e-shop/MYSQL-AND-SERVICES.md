# SQL linkage: one database for all services

All six MySQL-backed microservices use **one database**: **`eshop`**. Use this when running the app with MySQL (e.g. on the demo laptop).

---

## One database: eshop

| Service             | Database | Tables (in eshop) |
|---------------------|----------|-------------------|
| **auth-service**    | `eshop`  | user_credentials, refresh_tokens, roles, user_roles |
| **user-service**    | `eshop`  | user_profiles |
| **product-service** | `eshop`  | categories, products |
| **order-service**   | `eshop`  | orders, order_items |
| **inventory-service** | `eshop` | stock |
| **payment-service** | `eshop`  | payments |

**Cart service** uses **MongoDB** (`cart_db`), not MySQL — no SQL script.

---

## Steps to use MySQL (one database)

### 1. Create the single database and all tables

1. Start **MySQL** (e.g. MySQL Server service).
2. In MySQL Workbench (or any client), run **once**:
   - **`init-db/01-single-database-all-tables.sql`** — creates database **`eshop`** and all tables used by auth, user, product, order, inventory, and payment services.

If you skip this script, each service will still create/update its tables on first start (JPA `ddl-auto: update`) when you use the **mysql** profile, but running the script is recommended.

### 2. Start services with the `mysql` profile

Each of the six services above must be run with **Spring profile `mysql`** so they use MySQL instead of H2.

**From command line (from project root):**

```bash
cd auth-service         && mvn spring-boot:run -Dspring-boot.run.profiles=mysql
cd user-service         && mvn spring-boot:run -Dspring-boot.run.profiles=mysql
cd product-service      && mvn spring-boot:run -Dspring-boot.run.profiles=mysql
cd order-service        && mvn spring-boot:run -Dspring-boot.run.profiles=mysql
cd inventory-service    && mvn spring-boot:run -Dspring-boot.run.profiles=mysql
cd payment-service      && mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

**From IntelliJ:**

1. Run each service’s main class (e.g. `AuthServiceApplication`) once so a run configuration exists.
2. **Run → Edit Configurations**.
3. For each of the six services (auth, user, product, order, inventory, payment):
   - Select its **Application** configuration.
   - In **Active profiles** (or **Environment → Program arguments**), add: **`mysql`**.
   - Or in **VM options** add: **`-Dspring.profiles.active=mysql`**.
4. Click **Apply** then **OK**. Next time you run that service, it will use MySQL.

### 3. Credentials and env vars

- **Default:** MySQL user **`root`**, password **`root`**, host **localhost**, port **3306**.
- **Override** (e.g. on corporate laptop): set before starting the app:
  - `MYSQL_HOST` (e.g. localhost)
  - `MYSQL_PORT` (e.g. 3306)
  - `MYSQL_USER`
  - `MYSQL_PASSWORD`

In IntelliJ you can set these in **Edit Configurations → Environment variables** for each service.

---

## Quick checklist (other laptop)

- [ ] MySQL installed and running.
- [ ] Ran **`01-single-database-all-tables.sql`** once (creates `eshop` and all tables).
- [ ] Started each of the six services **with profile `mysql`** (command line or IntelliJ).
- [ ] Cart service and API Gateway do **not** need the mysql profile (Cart uses MongoDB; Gateway has no DB).

For full setup (zip, Java, Maven, MongoDB, Postman), see **RUN-ON-DEMO-LAPTOP.md** and **init-db/README.md**.
