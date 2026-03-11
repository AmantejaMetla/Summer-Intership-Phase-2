# Setup on another laptop – full implementation guide

Use this guide **on the laptop where you want to run the project** (e.g. after transferring from your main machine). It covers getting the updated code (including everything we did after Stripe), installing prerequisites, and running the app so you can test the frontend and APIs.

**If the other laptop has a zip “till Stripe”:** The project has since been updated (Razorpay instead of Stripe, merchant role, dataset seed, React hero/shop). You need the **current project** on that laptop. The easiest way is to create a new zip from **this laptop** (where all changes are done) and extract it on the other laptop. Then follow the steps below. No need to “patch” the old zip.

---

## What’s in the current project (after today’s work)

- **Payments:** Razorpay (Stripe removed). See **monday.md** for keys and testing.
- **Merchant role:** Sellers can manage their products and see sales. See **MERCHANT-ROLE.md** if needed.
- **Data:** One MySQL database `eshop`; product seed from coffee-shop dataset (24 products, 3 categories). Optional orders seed from dataset.
- **React frontend:** Hero with product carousel and floating beans, shop page with left filters (category, price, in stock), sort, active filter pills. API proxy to gateway.
- **Backend:** Eureka, API Gateway, auth, user, product, order, cart (MongoDB), inventory, payment — all as before, with profile `mysql` for MySQL.

---

## Step 1 – Get the project onto the other laptop

**On this laptop (where you have the full project):**

1. Go to the **parent** of the project folder (e.g. `Documents\!Practice\`).
2. Right‑click **E-Shopping Case Study** → **Send to → Compressed (zipped) folder**.
3. Name it e.g. **E-Shopping-Case-Study-Transfer.zip** and copy it to the other laptop (USB, cloud, etc.).

**On the other laptop:**

1. Extract the zip so you have a folder **E-Shopping Case Study** with `eureka-server`, `api-gateway`, `auth-service`, `frontend`, `init-db`, `postman`, etc. inside it.
2. Open that folder in your editor and use it as the **project root** for all commands below.

---

## Step 2 – Prerequisites

| Need        | Purpose                    |
|------------|----------------------------|
| **Java 17+** | Run Spring Boot services   |
| **Maven**    | Build and run backend      |
| **MySQL 8** or **XAMPP** | Database (required for auth, user, product, order, inventory, payment) |
| **Node.js 18+** | Run React frontend (optional) |

Check: `java -version`, `mvn -v`, `node -v` (if using frontend).

---

## Step 3 – MySQL: start and create database

1. **Start MySQL**
   - **XAMPP:** Control Panel → **Start** next to MySQL.
   - **Windows (MySQL installer):** `Win + R` → `services.msc` → find **MySQL** / **MySQL80** → Right‑click → **Start**.

2. **Create database and tables** (once per machine)

   From the **project root** in a terminal (PowerShell):

   ```powershell
   Get-Content "init-db\01-single-database-all-tables.sql" | mysql -u root -p
   ```
   Enter your MySQL root password. (XAMPP often has no password — press Enter.)

3. **Optional – product images column** (if your script set doesn’t have it):
   ```powershell
   Get-Content "init-db\05-alter-products-image-url.sql" | mysql -u root -p eshop
   ```

4. **Optional – seed products (24 coffee-shop items):**
   ```powershell
   Get-Content "init-db\06-seed-products-from-dataset.sql" | mysql -u root -p eshop
   ```
   If you skip this, product-service can still auto-load from `product-seed.json` when the `products` table is empty.

5. **Optional – seed orders from dataset:**  
   If you have `init-db\07-seed-orders-from-dataset.sql`, run it **after** 06:
   ```powershell
   Get-Content "init-db\07-seed-orders-from-dataset.sql" | mysql -u root -p eshop
   ```

6. **If the `payments` table already existed before Razorpay:**  
   Run `init-db\03-alter-payments-razorpay-columns.sql` so the new payment columns exist. See **monday.md**.

The app expects MySQL user **root**, password **root**, database **eshop**. If your password is different, set it when starting services (Step 5).  
**Troubleshooting:** **MYSQL-SETUP.md**.

---

## Step 4 – Set environment variables (if needed)

In PowerShell (same session you’ll use to start services):

- **MySQL password not `root`:**
  ```powershell
  $env:MYSQL_PASSWORD="your_mysql_password"
  ```
- **Razorpay (for payment tests):** See **monday.md**. Before starting payment-service:
  ```powershell
  $env:RAZORPAY_KEY_ID="rzp_test_xxxx"
  $env:RAZORPAY_KEY_SECRET="your_secret"
  ```

### No-admin OTP setup (recommended for corporate laptops)

Use the project-local auth launcher instead of Windows-level `setx`.

1. Copy template and fill real SMTP values:
   ```powershell
   Copy-Item "tools\auth-service.env.example" "tools\auth-service.env.local"
   ```
2. Edit `tools\auth-service.env.local` and set:
   - `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM`
3. Start auth-service with local env:
   ```powershell
   .\tools\start-auth-service.ps1
   ```
4. If you are running MySQL profile:
   ```powershell
   .\tools\start-auth-service.ps1 -UseMysqlProfile
   ```

`tools\auth-service.env.local` is ignored by git, so secrets stay on that laptop.

---

## Step 5 – Start the backend (8 terminals)

Open **8 separate terminals**. From the **project root** in each, run **one** of these.  
Use **`-Dspring.profiles.active=mysql`** with **no quotes** so Maven doesn't report "Unknown lifecycle phase".

**Terminal 1 – Eureka**
```powershell
cd eureka-server
mvn spring-boot:run
```
Wait until you see “Started EurekaServerApplication”.

**Terminal 2 – API Gateway**
```powershell
cd api-gateway
mvn spring-boot:run
```

**Terminals 3–8 – Services (one per terminal)**
```powershell
cd auth-service; mvn spring-boot:run -Dspring.profiles.active=mysql
cd user-service; mvn spring-boot:run -Dspring.profiles.active=mysql
cd product-service; mvn spring-boot:run -Dspring.profiles.active=mysql
cd order-service; mvn spring-boot:run -Dspring.profiles.active=mysql
cd inventory-service; mvn spring-boot:run -Dspring.profiles.active=mysql
cd payment-service; mvn spring-boot:run -Dspring.profiles.active=mysql
```
**Cart (optional, needs MongoDB):**
```powershell
cd cart-service; mvn spring-boot:run -Dspring.profiles.active=mysql
```

Give services 1–2 minutes to start.

### Run in IDE terminals (VS Code / Cursor / IntelliJ)

**See [IDE-TERMINALS.md](IDE-TERMINALS.md)** for copy-paste commands for each of the 9 terminals. Use **`-Dspring.profiles.active=mysql`** (no quotes) for MySQL-linked services to avoid the "Unknown lifecycle phase" Maven error. If only **API-GATEWAY** and **CART** appear in Eureka, the others are likely failing due to MySQL (start MySQL and run Step 3, then restart those services).

---

## Step 6 – Start the React frontend

**Option A – One script (backend + frontend):**  
From project root in PowerShell:

```powershell
.\start-all-services.ps1
```

This opens 9 windows: Eureka, Gateway, Auth, User, Product, Order, Inventory, Payment, and the **frontend**. The frontend window will run `npm install` once if needed, then `npm run dev`. Open **http://localhost:3000** when the frontend window shows “Local: http://localhost:3000”.

**Option B – Manual:**  
New terminal, from project root:

```powershell
cd frontend
npm install
npm run dev
```

Open **http://localhost:3000** in the browser.

---

## Step 7 – Verify everything

| Check | Where / what |
|-------|------------------|
| **Eureka** | http://localhost:8761 — expect API-GATEWAY, AUTH-SERVICE, USER-SERVICE, PRODUCT-SERVICE, ORDER-SERVICE, INVENTORY-SERVICE, PAYMENT-SERVICE (and CART if you started it). |
| **Gateway** | http://localhost:9090/actuator/health — status UP. |
| **Products API** | http://localhost:9090/api/products — JSON array (or empty if no seed). |
| **Frontend** | http://localhost:3000 — Home (hero, “Shop now”), Shop (filters, product grid). No “Can’t reach the server” if backend is up. |
| **Payments** | Postman: Create order (Razorpay), List my payments. See **monday.md**. |

---

## Troubleshooting – Frontend integration failed

| Symptom | What to do |
|--------|------------|
| **Frontend window: “npm is not recognized”** | Install **Node.js 18+** from nodejs.org. Close the frontend window, run `.\start-all-services.ps1` again or run `cd frontend` then `npm install` and `npm run dev`. |
| **Frontend window: “Cannot find module” or ENOENT** | In the frontend window (or a new terminal): `cd frontend` then `npm install`, then `npm run dev`. |
| **Browser: “Can’t reach the server” on Home/Shop** | Backend or gateway not ready. Check Eureka (http://localhost:8761) shows API-GATEWAY and PRODUCT-SERVICE. Check http://localhost:9090/actuator/health is UP. Start MySQL and run init scripts if services fail to register. |
| **Port 3000 already in use** | Stop the other app using 3000, or set another port: in `frontend` run `npm run dev -- --port 3001` and open http://localhost:3001. |
| **Only some backends in Eureka** | Usually MySQL not running or wrong password. Start MySQL, run **01-single-database-all-tables.sql**, set `$env:MYSQL_PASSWORD` if not `root`, restart the service windows that failed. |
| **Maven: "Unknown lifecycle phase .run.profiles=mysql"** | Use **`-Dspring.profiles.active=mysql`** with **no quotes**. See [IDE-TERMINALS.md](IDE-TERMINALS.md) for copy-paste commands. |

---

## Step 8 – Optional docs

- **Razorpay:** **monday.md**
- **Merchant role and APIs:** **MERCHANT-ROLE.md**
- **React structure and concepts:** **REACT-IMPLEMENTATION-GUIDE.md**
- **Dataset and seed mapping:** **dataset/DATASET-MAPPING.md**
- **Product images and labels:** **IMAGES-FOR-FRONTEND.md**
- **MySQL issues:** **MYSQL-SETUP.md**
- **Init scripts overview:** **init-db/README.md**

---

## Summary checklist (other laptop)

- [ ] Extracted the **current** project zip (with Razorpay, merchant, dataset, React updates).
- [ ] Java 17+ and Maven installed.
- [ ] MySQL running; ran **01-single-database-all-tables.sql**; optionally 05, 06, 07.
- [ ] If payments table was old: ran **03-alter-payments-razorpay-columns.sql**.
- [ ] Set `MYSQL_PASSWORD` (and Razorpay env vars if testing payments).
- [ ] Started backend + frontend: run `.\start-all-services.ps1` (or start Eureka → Gateway → 6 services manually, then `cd frontend` → `npm install` → `npm run dev`).
- [ ] Eureka shows all services; gateway health UP; `/api/products` returns data.
- [ ] Opened http://localhost:3000 and tested Home + Shop (no “Can’t reach the server”).

---

## GitHub transfer checklist (before push)

Use this every time before uploading the project to GitHub:

- [ ] `tools/auth-service.env.local` is **not** committed (secrets file).
- [ ] `tools/mysql.env.local` is **not** committed (local DB password).
- [ ] `payment-service/razorpay-keys.yml` is **not** committed.
- [ ] Only template files are committed:
  - `tools/auth-service.env.example`
  - `tools/mysql.env.example`
- [ ] If any secret was shared in chat/history, rotate it (SMTP, Twilio, Razorpay).

### Quick GitHub flow (if this folder is not yet a repo)

```powershell
cd "C:\Users\amant\OneDrive\Documents\!Practice\E-Shopping Case Study"
git init
git add .
git commit -m "Initial case-study handoff with setup docs"
git branch -M main
git remote add origin <your-github-repo-url>
git push -u origin main
```

---

## OTP/SMS setup matrix for another laptop

Pick one mode in `tools/auth-service.env.local`:

### A) Most reliable demo mode (recommended for case study)

```env
SMS_ENABLED=true
SMS_PROVIDER=log
OTP_RABBIT_ENABLED=false
```

Behavior:
- Email OTP works via SMTP.
- SMS option is simulated in logs (no paid telecom setup needed).

### B) Twilio mode (real SMS on trial/verified numbers)

```env
SMS_ENABLED=true
SMS_PROVIDER=twilio
TWILIO_BASE_URL=https://api.twilio.com/2010-04-01
TWILIO_ACCOUNT_SID=AC...
TWILIO_AUTH_TOKEN=...
TWILIO_FROM_NUMBER=+1...
```

Notes:
- Trial accounts generally send only to verified numbers.
- If SMS fails, auth-service falls back to email OTP when possible.

### C) RabbitMQ enabled (optional reliability layer)

```env
OTP_RABBIT_ENABLED=true
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASS=guest
OTP_RABBIT_EXCHANGE=auth.otp.exchange
OTP_RABBIT_QUEUE=auth.otp.queue
OTP_RABBIT_ROUTING_KEY=auth.otp
```

Use this only if RabbitMQ is installed/running on that laptop.  
If unavailable, keep `OTP_RABBIT_ENABLED=false`.
