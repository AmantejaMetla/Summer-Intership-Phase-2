# Run all services in IDE terminals (VS Code / Cursor / IntelliJ)

Open the **project root** in your IDE (the folder that contains `eureka-server`, `auth-service`, `frontend`, etc.). Open **9 separate terminals**; each usually starts at project root.

In each terminal, **paste only the line for that terminal**.

- **Must start from project root.** If your prompt shows `\auth-service` or `\product-service`, run `cd ..` until you see `E-Shopping Case Study` in the path, then paste the command.

---

## Option A – No MySQL (use this if you don't have / don't remember MySQL password)

Services use **H2 in-memory** databases. No MySQL setup, no password. Data does not persist after you stop the service.

**Terminals 3–8:** use the **simple** commands below (no `$env:SPRING_PROFILES_ACTIVE`, no MySQL).

---

## Option B – With MySQL (persistent data)

Use the commands that set `$env:SPRING_PROFILES_ACTIVE="mysql"` and ensure MySQL is running with user `root` and password `root` (or set `$env:MYSQL_PASSWORD="YourPassword"` in each terminal 3–8). See Troubleshooting if you get "Access denied".

---

## Terminal 1 – Eureka
```powershell
cd eureka-server; mvn spring-boot:run
```
Wait until you see "Started EurekaServerApplication" before starting Gateway.

---

## Terminal 2 – API Gateway
```powershell
cd api-gateway; mvn spring-boot:run
```

---

## Terminal 3 – Auth
**Option A (no MySQL):**
```powershell
cd auth-service; mvn spring-boot:run
```
**Option B (with MySQL):**
```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"; cd auth-service; mvn spring-boot:run
```
(Set `$env:MYSQL_PASSWORD="YourPassword"` first if your MySQL password is not `root`.)

---

## Terminal 4 – User
**Option A (no MySQL):** `cd user-service; mvn spring-boot:run`  
**Option B (MySQL):** `$env:SPRING_PROFILES_ACTIVE="mysql"; cd user-service; mvn spring-boot:run`

---

## Terminal 5 – Product
**Option A (no MySQL):** `cd product-service; mvn spring-boot:run`  
**Option B (MySQL):** `$env:SPRING_PROFILES_ACTIVE="mysql"; cd product-service; mvn spring-boot:run`

---

## Terminal 6 – Order
**Option A (no MySQL):** `cd order-service; mvn spring-boot:run`  
**Option B (MySQL):** `$env:SPRING_PROFILES_ACTIVE="mysql"; cd order-service; mvn spring-boot:run`

---

## Terminal 7 – Inventory
**Option A (no MySQL):** `cd inventory-service; mvn spring-boot:run`  
**Option B (MySQL):** `$env:SPRING_PROFILES_ACTIVE="mysql"; cd inventory-service; mvn spring-boot:run`

---

## Terminal 8 – Payment
**Option A (no MySQL):** `cd payment-service; mvn spring-boot:run`  
**Option B (MySQL):** `$env:SPRING_PROFILES_ACTIVE="mysql"; cd payment-service; mvn spring-boot:run`

---

## Terminal 9 – Frontend
```powershell
cd frontend; npm install; npm run dev
```
Then open http://localhost:3000

---

## Order

Start **1 → 2**, then 3–8 in any order. Start **9** (frontend) whenever you like.

## Troubleshooting

 didn’| Problem | Fix |
|--------|-----|
| **"Unknown lifecycle phase"** | Use the `$env:SPRING_PROFILES_ACTIVE="mysql"; cd ...` form above (no `-D` in PowerShell). |
| **"Cannot find path ...auth-serviceauth-service"** | Run `cd ..` until the prompt shows `E-Shopping Case Study`, then run the command again. |
| **"Access denied for user root"** | Use **Option A (no MySQL)** above: run `cd auth-service; mvn spring-boot:run` (and same for user, product, order, inventory, payment) with **no** `$env:SPRING_PROFILES_ACTIVE="mysql"`. No MySQL or password needed. |
