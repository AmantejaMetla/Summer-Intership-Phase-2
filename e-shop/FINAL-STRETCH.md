# Final Stretch Runbook

Use this file as the single source of truth for the final phase.
It captures what was implemented, how to run it, and how to troubleshoot quickly.

---

## What Was Added Today

- OTP email flow wired in `auth-service` with SMTP support.
- RabbitMQ-backed OTP dispatch added (optional toggle with safe fallback).
- OTP channel preference added: users can choose `EMAIL` or `SMS` for OTP dispatch.
- Fallback OTP logging added when email send fails (dev safety net).
- TOTP (Google Authenticator) dev bypass added (`000000`) as backup.
- Register flow upgraded to show QR code for Google Authenticator scan.
- Forgot Password flow improved:
  - clear "email not registered" response
  - back/start-over UX to avoid softlock
  - register/login links added
- Portable auth startup for restricted laptops:
  - `tools/start-auth-service.ps1`
  - `tools/auth-service.env.example`
  - `tools/auth-service.env.local` (local secrets file)
- Startup automation improvements:
  - `start-all-services.ps1` now loads `tools/mysql.env.local` and `tools/auth-service.env.local`
  - `start-all-services.ps1` now includes `cart-service`
- Gateway auth hardening:
  - only `GET /api/products/**` is public
  - product create/update/delete routes now require JWT
- Product catalog persistence fixes for MySQL:
  - `init-db/11-refresh-product-prices-and-images.sql`
  - `init-db/12-force-refresh-product-catalog.sql` (fallback without temp tables)
- Delivery simulation MVP:
  - order statuses now include delivery flow stages
  - backend simulation start endpoint
  - scheduler auto-advances order stages
  - cart page shows delivery progress/timeline
- Real-time delivery updates:
  - order-service STOMP broker endpoint at `/ws-orders` (SockJS)
  - topic push per order: `/topic/orders/{orderId}`
  - cart page subscribes live and keeps polling as fallback
- Role application flow:
  - new frontend tab/page: `/apply-role`
  - users can apply for `MERCHANT` or `DELIVERY_AGENT`
  - simple credential checks for auto-approve vs under-review
  - approved applications grant role in `user_roles`
  - admin review APIs for manual decisions
- Simulated delivery map:
  - cart tracking now shows rider movement on a mock map
  - distance remaining is calculated from progress percent

---

## Current Auth Flow (Expected Behavior)

### Register
1. User enters email + password.
2. OTP channel is chosen (`EMAIL`/`SMS`), OTP is generated and sent.
3. User verifies email OTP.
4. User sets up Google Authenticator (QR scan or manual secret).
5. User confirms TOTP code.

### Login
- Login does **not** send a new OTP email.
- User enters:
  - email
  - password
  - current Google Authenticator 6-digit code

### Forgot Password
1. Request OTP with email.
2. If email is unregistered: API returns `Email is not registered`.
3. If registered: OTP sent on selected channel; for SMS, phone must match stored number.
4. User resets password with reset token + TOTP code.

---

## Important Dev Backups (Keep Until Final Demo)

- **Email OTP fallback log**  
  If SMTP fails, auth logs include OTP message for dev testing.

- **TOTP dev bypass**  
  `TOTP_DEV_BYPASS_CODE=000000` allows 2FA checks to pass in local/dev.

Disable both before production handoff.

---

## Portable Setup (No Admin Rights / Corporate Laptop Friendly)

Do **not** rely on machine-level `setx`.
Use local file + script:

1. Copy template:
   ```powershell
   Copy-Item "tools\auth-service.env.example" "tools\auth-service.env.local"
   ```
2. Fill values in `tools/auth-service.env.local`.
3. Start auth-service:
   ```powershell
   .\tools\start-auth-service.ps1
   ```
4. If running MySQL profile:
   ```powershell
   .\tools\start-auth-service.ps1 -UseMysqlProfile
   ```

This keeps setup local to the project and avoids admin/system config changes.

---

## MySQL Setup For Persistent SQL Writes

To make data persist in SQL tables (not H2), mysql-profile services must connect successfully.

1. Copy:
   ```powershell
   Copy-Item "tools\mysql.env.example" "tools\mysql.env.local"
   ```
2. Fill `tools/mysql.env.local` with real credentials.
3. Start stack:
   ```powershell
   .\start-all-services.ps1
   ```
4. Verify service logs show `jdbc:mysql://.../eshop` (not `jdbc:h2:mem...`).

If you see `Access denied for user 'root'@'localhost'`, your MySQL password/user in `tools/mysql.env.local` is wrong.

If product prices/image URLs look reset after DB migration, run:
```powershell
Get-Content "init-db\11-refresh-product-prices-and-images.sql" | mysql -u root -p eshop
```
This restores curated prices and local image paths (`/images/*.jpg`).

---

## SMTP Provider Choice

Current recommended provider: **Brevo** (free tier friendly for OTP testing).

Required values:
- `SMTP_HOST`
- `SMTP_PORT`
- `SMTP_USER`
- `SMTP_PASS`
- `SMTP_FROM`
- `SMTP_AUTH=true`
- `SMTP_STARTTLS_ENABLE=true`

`SMTP_FROM` must be a verified sender in Brevo.

---

## RabbitMQ OTP (Reliability Layer)

This is optional and controlled by env vars in `tools/auth-service.env.local`.

Required/related keys:
- `OTP_RABBIT_ENABLED` (`true` or `false`)
- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USER`
- `RABBITMQ_PASS`
- `OTP_RABBIT_EXCHANGE`
- `OTP_RABBIT_QUEUE`
- `OTP_RABBIT_ROUTING_KEY`

Behavior:
- If enabled and RabbitMQ is reachable: OTP requests are queued first, then consumed and emailed.
- If publish fails (broker down/network issue): auth-service automatically falls back to direct SMTP send.
- If SMTP also fails: OTP is still logged in auth-service logs for local dev continuity.

This prevents "hard lock" incidents during login/register troubleshooting.

---

## SMS Provider (MSG91)

OTP now supports real SMS delivery for Indian numbers via MSG91.

Set in `tools/auth-service.env.local`:
- `SMS_ENABLED=true`
- `SMS_PROVIDER=msg91`
- `MSG91_AUTH_KEY=<your-msg91-auth-key>`
- `MSG91_SENDER_ID=<approved-6-char-sender-id>`
- `MSG91_ROUTE=4`

Optional:
- `MSG91_BASE_URL=https://control.msg91.com/api/sendhttp.php`

If SMS send fails, system automatically falls back to email OTP (if email exists).

---

## SMS Provider (Twilio)

OTP also supports Twilio.

Set in `tools/auth-service.env.local`:
- `SMS_ENABLED=true`
- `SMS_PROVIDER=twilio`
- `TWILIO_BASE_URL=https://api.twilio.com/2010-04-01`
- `TWILIO_ACCOUNT_SID=<your-account-sid>`
- `TWILIO_AUTH_TOKEN=<your-auth-token>`
- `TWILIO_FROM_NUMBER=<your-twilio-number>`

Notes:
- Trial accounts usually deliver only to verified recipient numbers.
- If Twilio SMS fails, system automatically falls back to email OTP (if email exists).

---

## Run Commands (Quick)

### Start auth-service using local env
```powershell
cd "C:\Users\amant\OneDrive\Documents\!Practice\E-Shopping Case Study"
.\tools\start-auth-service.ps1
```

### Start full stack (now includes cart-service)
```powershell
cd "C:\Users\amant\OneDrive\Documents\!Practice\E-Shopping Case Study"
.\start-all-services.ps1
```

`start-all-services.ps1` auto-loads:
- `tools/mysql.env.local` (MySQL credentials)
- `tools/auth-service.env.local` (Brevo SMTP + TOTP dev config)

### Start auth-service with MySQL profile
```powershell
cd "C:\Users\amant\OneDrive\Documents\!Practice\E-Shopping Case Study"
.\tools\start-auth-service.ps1 -UseMysqlProfile
```

---

## Troubleshooting Fast Map

### 1) "Service Unavailable" on frontend auth screens
- Usually gateway/auth not ready, or frontend stale.
- Check:
  - Eureka up (`8761`)
  - Gateway up (`9090`)
  - Auth up (`9081`)
- Hard refresh frontend (`Ctrl+F5`).

### 2) "Did not receive OTP email"
- Check auth logs:
  - If Rabbit enabled and broker is down, you will see fallback warning and direct send attempt.
  - If `Authentication failed` -> SMTP credentials mismatch/invalid.
  - If fallback line appears -> use logged OTP to continue in dev.
- Confirm Brevo sender verification.

### 2b) "Did not receive OTP SMS"
- Check `SMS_PROVIDER` in `tools/auth-service.env.local`.
- For Twilio:
  - verify `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`
  - ensure recipient number is verified if account is trial
- For demo mode, set `SMS_PROVIDER=log` and check auth-service logs for OTP text.

### 3) Login page "not sending OTP"
- Expected by design.
- Login uses TOTP input (Authenticator code), not email OTP dispatch.

### 4) 2FA code rejected
- Ensure device time is automatic/synced.
- Retry with latest code.
- Use dev bypass `000000` (if enabled) while troubleshooting.

---

## Security Notes

- `tools/auth-service.env.local` contains secrets. Keep it private.
- It is gitignored and should not be committed.
- If secrets were shared in chat/history, rotate keys/passwords afterward.

---

## Final Demo Checklist

- [ ] Register -> email OTP -> TOTP setup via QR -> confirm
- [ ] Login with authenticator code
- [ ] Forgot Password: registered email path works end-to-end
- [ ] Forgot Password: unregistered email shows proper error
- [ ] SMTP emails are delivered without auth errors
- [ ] Dev bypass still available as fallback (`000000`)
- [ ] Corporate laptop can run via `tools/start-auth-service.ps1`

---

## Keep Updating This File

When new features/fixes are added, append:
- what changed
- setup steps
- commands
- known issues and quick fixes

This keeps final handoff simple and repeatable.
