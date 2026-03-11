# MySQL setup – fix “cannot connect” to database

The app connects to **MySQL** when you run services with the **mysql** profile. If you see “cannot connect” or “Communications link failure”, follow these steps.

**Why do I only see API-GATEWAY and CART in Eureka?**  
Gateway and Cart don’t use MySQL (Cart uses MongoDB). The other services (auth, user, product, order, inventory, payment) need MySQL to be **running** and the **eshop** database to exist. If MySQL isn’t running, those services fail on startup with “Communications link failure” and never register with Eureka. Start MySQL and run the init scripts (see below), then start the services again.

---

## 1. Install MySQL (if not installed)

- **Option A – MySQL Community Server**  
  Download: [https://dev.mysql.com/downloads/mysql/](https://dev.mysql.com/downloads/mysql/)  
  Run the installer. Remember the **root password** you set.

- **Option B – XAMPP**  
  Install [XAMPP](https://www.apachefriends.org/), which includes MySQL. Start MySQL from the XAMPP Control Panel.

- **Option B – WAMP / MAMP**  
  Same idea: install and start the MySQL service from the control panel.

---

## 2. Start the MySQL service (Windows)

- **If you used the official MySQL installer:**  
  - Open **Services** (Win + R → `services.msc` → Enter).  
  - Find **MySQL** or **MySQL80** (version may vary).  
  - Right‑click → **Start** (and set Startup type to **Automatic** if you want).

- **If you use XAMPP:**  
  - Open XAMPP Control Panel → click **Start** next to **MySQL**.

Check that it’s running: the status should show “Running” or “Started”.

---

## 3. Create the database and tables

The app expects a database named **eshop** and tables created from our script.

1. Open a terminal/command prompt and go to your project folder (where the `init-db` folder is).

2. Log in to MySQL (use the password you set for `root`):

   ```bash
   mysql -u root -p
   ```

   If MySQL is not in your PATH (e.g. with XAMPP), use the full path, for example:

   ```text
   C:\xampp\mysql\bin\mysql -u root -p
   ```

   Or open **MySQL command line** / **phpMyAdmin** (XAMPP) and run the script from there.

3. Run the init script so the `eshop` database and all tables are created:

   ```bash
   mysql -u root -p < init-db/01-single-database-all-tables.sql
   ```

   Or inside the MySQL prompt:

   ```sql
   source C:/full/path/to/your/project/init-db/01-single-database-all-tables.sql
   ```

   (Use forward slashes in the path, or the path MySQL expects.)

4. Check that the database exists:

   ```sql
   USE eshop;
   SHOW TABLES;
   ```

   You should see tables like `user_credentials`, `products`, `orders`, etc.

---

## 4. Use the same username and password as the app

The app uses these **defaults** (see each service’s `application-mysql.yml`):

| Setting   | Default value |
|----------|----------------|
| Host     | `localhost`    |
| Port     | `3306`         |
| Database | `eshop`        |
| Username | `root`        |
| Password | `root`         |

- If your MySQL **root password is different** (e.g. you set it to `mypass`), you must pass it when starting the services (see step 5).
- If your MySQL is on another machine or port, set `MYSQL_HOST` and `MYSQL_PORT` when starting the services.

---

## 5. Start the Spring Boot services with the mysql profile

Each service (auth, user, product, order, cart, inventory, payment, etc.) must run with **profile `mysql`** so they use MySQL instead of in‑memory H2.

**If your MySQL password is not `root`**, set it when starting. Examples:

**PowerShell:**

```powershell
$env:MYSQL_PASSWORD="your_actual_mysql_password"
cd auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

**Command Prompt (cmd):**

```cmd
set MYSQL_PASSWORD=your_actual_mysql_password
cd auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Use the **same** `MYSQL_PASSWORD` (and `MYSQL_USER`/`MYSQL_HOST`/`MYSQL_PORT` if needed) for every service.

Start **Eureka** first, then the **API gateway**, then the rest (auth, user, product, order, cart, inventory, payment).

---

## 6. Quick checklist

- [ ] MySQL is **installed** and the **MySQL service is running**.
- [ ] You ran **init-db/01-single-database-all-tables.sql** and the **eshop** database and tables exist.
- [ ] **Username/password** match: app uses `root` / `root` by default; if your MySQL uses a different password, you set `MYSQL_PASSWORD` (and optionally `MYSQL_USER`) when starting the app.
- [ ] Each service is started with **profile `mysql`** (e.g. `-Dspring-boot.run.profiles=mysql`).

---

## 7. If it still doesn’t connect

- **Error: “Access denied for user 'root'@'localhost'”**  
  Your MySQL password is not `root`. Set `MYSQL_PASSWORD` to your real root password when starting the services (see step 5).

- **Error: “Unknown database 'eshop'”**  
  The init script was not run or failed. Run **init-db/01-single-database-all-tables.sql** again and check for errors (step 3).

- **Error: “Communications link failure” or “Connection refused”**  
  MySQL is not running or not reachable. Check that the MySQL service is started (step 2) and that nothing else is using port **3306**. You can test with:

  ```bash
  mysql -u root -p -h 127.0.0.1 -P 3306 -e "SELECT 1"
  ```

  If this works, the app should be able to connect with the same host/port/user/password.

- **Different host/port:**  
  If MySQL is on another PC or port, set when starting the app:
  - `MYSQL_HOST=your_host`
  - `MYSQL_PORT=3307` (or whatever port you use)

After MySQL is running, the database and tables exist, and the app is started with the mysql profile and correct password, the “cannot connect” error should be resolved.
