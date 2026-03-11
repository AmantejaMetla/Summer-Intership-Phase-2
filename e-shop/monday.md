# Razorpay payment integration

This document covers **Razorpay** setup and testing for the E-Shop. Razorpay supports Indian payments (INR, UPI, cards, netbanking, wallets). **Test mode** does not require company verification — use test keys from the dashboard.

---

## 1. Flow

1. **Create order** – App calls `POST /api/payments/create-order` with `orderId` and `amount` (INR). A **PENDING** payment is saved and a Razorpay order is created. Response includes **razorpayOrderId** and **razorpayKeyId**.
2. **Checkout** – Frontend opens Razorpay Checkout (Razorpay.js with razorpayKeyId and razorpayOrderId). User pays (card/UPI/wallet).
3. **Verify** – Frontend receives `razorpay_payment_id`, `razorpay_order_id`, `razorpay_signature` and calls `POST /api/payments/verify` with **paymentId**, **razorpayOrderId**, **razorpayPaymentId**, **razorpaySignature**. Backend verifies the signature and updates the payment to **COMPLETED**, stores **gateway_payment_id** and **receipt_url** in the DB.

**Database:** Payment table has **gateway_order_id**, **gateway_payment_id**, **receipt_url**, **payment_method** (RAZORPAY).

---

## 2. Get Razorpay keys (test mode)

1. Go to **[razorpay.com](https://razorpay.com)** and sign up or log in.
2. Open **Settings → API Keys** (or **Dashboard → API Keys**).
3. Turn on **Test mode**. Copy **Key Id** (starts with `rzp_test_...`) and **Key Secret**. Keep them private.

---

## 3. Database (if you already had a payments table)

If the `payments` table was created **before** the Razorpay update, run **`init-db/03-alter-payments-razorpay-columns.sql`** in MySQL. It adds: `gateway_order_id`, `gateway_payment_id`, `receipt_url`, `payment_method`. If you get "Duplicate column name", skip that line.  
If you run **`init-db/01-single-database-all-tables.sql`** from scratch, these columns are already there.

---

## 4. Set keys when starting payment-service

**Option A – Environment variables (recommended)**

Before starting payment-service (PowerShell):

```powershell
$env:RAZORPAY_KEY_ID="rzp_test_xxxx"
$env:RAZORPAY_KEY_SECRET="your_secret"
cd payment-service; mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

**Option B – application.yml** (do not commit real keys)

In `payment-service/src/main/resources/application.yml` (or profile):

```yaml
razorpay:
  key-id: rzp_test_xxxx
  key-secret: your_secret
  currency: INR
```

Restart payment-service after setting the keys.

---

## 5. Test from Postman (via gateway)

1. Start **Eureka**, **Gateway**, **auth-service**, and **payment-service** (with Razorpay keys set).
2. Postman base URL: **http://localhost:9090**.
3. **Auth → Login** (or Register). Use the token in **Authorization: Bearer {{accessToken}}**.
4. **Payment** folder:
   - **Create order (Razorpay)** – Body: `{"orderId": 1, "amount": 299.00}`. Expect **200** with `paymentId`, `razorpayOrderId`, `razorpayKeyId`. If **503**, Razorpay is not configured (check env vars and restart).
   - **List my payments** – You should see the payment with `status: "PENDING"` and `gatewayOrderId`.
5. **Verify** is used after the user pays (e.g. via React with Razorpay Checkout). For Postman-only testing you can skip verify unless you have callback data.

---

## 6. Check payments in MySQL

```sql
USE eshop;
SELECT id, order_id, user_id, amount, status, gateway_order_id, gateway_payment_id, receipt_url, payment_method
FROM payments ORDER BY id DESC;
```

After create-order you should see a row with `gateway_order_id` and `status = 'PENDING'`. After verify, `gateway_payment_id` and `receipt_url` are set and `status = 'COMPLETED'`.

---

## 7. Checklist

- [ ] Razorpay account; **Test mode** on; **Key Id** and **Key Secret** copied.
- [ ] If `payments` table already existed: ran **`init-db/03-alter-payments-razorpay-columns.sql`**.
- [ ] **RAZORPAY_KEY_ID** and **RAZORPAY_KEY_SECRET** set when starting payment-service.
- [ ] **Create order (Razorpay)** in Postman returns 200 with `razorpayOrderId`.
- [ ] **List my payments** shows the payment with `gatewayOrderId`.
