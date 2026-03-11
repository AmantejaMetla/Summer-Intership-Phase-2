# E-Commerce Microservices (Academic)

Microservices e-commerce project: Java 21, Spring Boot 3, Eureka, API Gateway, JWT, MySQL, MongoDB (Cart), Razorpay, React frontend.

---

## Setup on another laptop

**Use one guide:** **[SETUP-ON-ANOTHER-LAPTOP.md](SETUP-ON-ANOTHER-LAPTOP.md)**

That document has step-by-step instructions to:

- Get the project onto the other laptop (zip from this machine and extract there)
- Install prerequisites (Java, Maven, MySQL, Node optional)
- Create the database and optionally seed products/orders
- Start Eureka, Gateway, and all services (with PowerShell-friendly commands)
- Start the React frontend and verify it works

If the other laptop has an older zip (e.g. “till Stripe”), use a **new zip from this laptop** so it includes Razorpay, merchant role, dataset seed, and React updates. Then follow the same guide.

---

## Documentation (simple structure)

| Document | Use when |
|----------|----------|
| **[SETUP-ON-ANOTHER-LAPTOP.md](SETUP-ON-ANOTHER-LAPTOP.md)** | Setting up and running the project on another machine. |
| **[IDE-TERMINALS.md](IDE-TERMINALS.md)** | Running all services from IDE terminals (no extra PowerShell windows). |
| **[monday.md](monday.md)** | Razorpay: get keys, set env vars, test create-order and verify. |
| **[MYSQL-SETUP.md](MYSQL-SETUP.md)** | MySQL won’t connect; only Gateway/Cart in Eureka; fix credentials or service. |
| **[MERCHANT-ROLE.md](MERCHANT-ROLE.md)** | Merchant/seller APIs and how to assign the role. |
| **[REACT-IMPLEMENTATION-GUIDE.md](REACT-IMPLEMENTATION-GUIDE.md)** | How the React app is structured (routes, auth, shop, filters). |
| **[IMAGES-FOR-FRONTEND.md](IMAGES-FOR-FRONTEND.md)** | Which product images to add and how to name them. |
| **init-db/README.md** | Which SQL scripts exist and when to run them. |
| **dataset/DATASET-MAPPING.md** | How Kaggle/dataset files map to products, orders, and seed scripts. |
| **postman/README.md** | Import Postman collection and test APIs via gateway. |

Optional deeper reference: **SETUP.md**, **MYSQL-AND-SERVICES.md**, **SERVICES-AND-GATEWAY.md** (local run details and service/database mapping).

---

## Quick run (this machine)

1. Start **MySQL**; run `init-db/01-single-database-all-tables.sql` (and optionally `06-seed-products-from-dataset.sql`).
2. From project root: **`.\start-all-services.ps1`** — starts 8 backend windows + 1 frontend window (runs `npm install` in frontend if needed, then `npm run dev`). Wait 1–2 min for backends, then open http://localhost:3000.
3. Or start manually: Eureka → Gateway → auth, user, product, order, inventory, payment (each with `-Dspring-boot.run.profiles=mysql`); then `cd frontend` → `npm install` → `npm run dev`.

**Endpoints:** Eureka http://localhost:8761 · Gateway http://localhost:9090 · Frontend http://localhost:3000

**Prefer IDE terminals?** See **[IDE-TERMINALS.md](IDE-TERMINALS.md)** — one command per terminal, no extra PowerShell windows.

---

## Tech stack

- **Java 17+**, **Spring Boot 3**, **Maven**
- **Eureka** – service registry
- **Spring Cloud Gateway** – routes, JWT filter, CORS
- **MySQL** – single database `eshop` for auth, user, product, order, inventory, payment
- **MongoDB** – Cart service
- **Razorpay** – payments (see monday.md)
- **React (Vite)** – frontend; proxy `/api` to gateway
