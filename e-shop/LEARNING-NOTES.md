# Learning Notes ? E?Commerce Microservices

These notes explain **what we built**, **why we designed it this way**, and **how the main pieces of code work**, so you can learn from this project.

---

## 1. Big Picture

- **Goal**: Simple, academic microservices architecture for an e?commerce system.
- **Tech**: Java 21, Spring Boot 3, Spring Cloud (Eureka + Gateway), Spring Security + JWT, MySQL (relational services), MongoDB (cart only), Maven.
- **Services**:
  - `eureka-server`: service registry
  - `api-gateway`: single entry point, routing, JWT, simple rate limiting
  - `auth-service`: login/register, JWT access + DB?stored refresh tokens
  - `user-service`: profile data per user
  - `product-service`: product catalog
  - `cart-service`: shopping cart (MongoDB)
  - `order-service`: orders
  - `inventory-service`: stock levels
  - `payment-service`: payments

**Why microservices?**

- You can run/scale each part independently.
- Each service has its own database ? avoids tight coupling.
- Good for learning **service discovery**, **API gateways**, and **JWT?based auth**.

We intentionally **did NOT** add Kafka, Sagas, circuit breakers, tracing, etc., to keep it focused on fundamentals.

---

## 2. Service Registry ? `eureka-server`

**Main class** (`EurekaServerApplication`):

- Annotated with `@SpringBootApplication` and `@EnableEurekaServer`.
- Starts a Spring Boot app with the embedded server on port `8761`.
- Shows a dashboard at `http://localhost:8761` listing registered services.

**Config** (`eureka-server/src/main/resources/application.yml`):

- `server.port: 8761` ? registry port.
- `spring.application.name: eureka-server` ? logical service name.
- `eureka.client.register-with-eureka: false`, `fetch-registry: false` ? because this instance *is* the registry, it doesn?t register with itself.

**Why we need a registry**:

- Microservices move around (different ports, different machines).
- Instead of hardcoding `http://localhost:8081`, etc., services register in Eureka.
- Other services (and the gateway) just say `lb://auth-service` and Eureka resolves it.

---

## 3. API Gateway ? `api-gateway`

### 3.1 Purpose

The gateway is the **single entry point** for clients:

- Handles **routing**: `/api/auth/**` ? `auth-service`, `/api/products/**` ? `product-service`, etc.
- Applies **cross?cutting concerns** once:
  - JWT validation
  - Basic rate limiting
  - CORS

This avoids duplicating auth / CORS / rate?limiting logic in every microservice.

### 3.2 Routing config (`api-gateway/application.yml`)

Key ideas:

- `spring.cloud.gateway.discovery.locator.enabled: true` ? allows using `lb://SERVICE-NAME` and auto routes if needed.
- Each explicit route looks like:

  - `id`: logical id for the route  
  - `uri: lb://auth-service` ? use Eureka to resolve `auth-service` instances  
  - `predicates: Path=/api/auth/**` ? match incoming request path

**Why `lb://`?**

- `lb://auth-service` = "use the load balancer and Eureka to find `auth-service`".
- If you add more instances later, the gateway can balance across them.

### 3.3 JWT filter (`JwtAuthenticationGatewayFilterFactory`)

This is a **Spring Cloud Gateway filter factory**:

- Implemented by extending `AbstractGatewayFilterFactory<Config>`.
- Spring picks it up because of `@Component` and because the name matches the filter id (`JwtAuthentication`).

**Logic:**

1. Read the request path.
2. If the path starts with an **excluded prefix** (e.g. `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh`), **skip** JWT validation. These endpoints must be public.
3. Get `Authorization` header.  
   - If missing or doesn?t start with `Bearer ` ? respond `401 Unauthorized`.
4. Extract the token substring after `Bearer `.
5. Use `jjwt` with the shared secret (`jwt.secret`) to:
   - Parse and verify the token.
   - Read the subject (user id).
6. Mutate the request to add `X-User-Id` header, then forward it.

**Why header injection (`X-User-Id`)?**

- Downstream services don?t need to know how to parse JWT.
- They just read `X-User-Id` and trust that gateway already validated the token.

### 3.4 Simple rate limiting (`SimpleRateLimiterGatewayFilterFactory`)

We built a **very basic in?memory rate limiter**:

- Uses a `ConcurrentHashMap<clientKey, RequestWindow>` where:
  - `clientKey` = client IP.
  - `RequestWindow` = (count, resetAt).
- Each request:
  - Finds the client?s window.
  - If window expired ? reset count and window.
  - Else increment count.
- If `count > MAX_REQUESTS` (e.g. 20 per 60 seconds) ? return `429 Too Many Requests`.

**Why this simple version?**

- No extra infra (like Redis needed by `RequestRateLimiter`).
- Good enough for a learning project to show the concept.

---

## 4. Auth Service ? `auth-service`

### 4.1 What it does

- **Register**: create a new user credential, return access + refresh tokens.
- **Login**: check email + password, return tokens.
- **Refresh**: take a refresh token, validate it, issue a new access token and refresh token.

We separate **authentication** (Auth service) from **user profile** (User service) on purpose:

- Auth deals with security concerns (password hashing, tokens).
- User service holds profile info (name, address, etc.) and trusts `X-User-Id`.

### 4.2 Key entities

- `UserCredential` (JPA entity, `user_credentials` table)  
  - `email` (unique)  
  - `passwordHash` (BCrypt)
- `RefreshToken` (JPA entity, `refresh_tokens` table)  
  - `token` (random string)  
  - `userId`  
  - `expiryDate` (`Instant`)

**Why store refresh tokens in DB?**

- Lets you revoke tokens (delete by `userId`).
- Limits how long a refresh token is valid (`expiryDate`).
- Simpler than stateless refresh tokens for a learning project.

### 4.3 Services

- `JwtService`  
  - Holds the secret key and lifetime.  
  - Creates short?lived access tokens (`15 minutes`) with the user id as subject.
- `AuthService`  
  - `register(email, password)`:
    - If email exists, return empty.
    - Hash password with `PasswordEncoder`.
    - Save `UserCredential`.
    - Create access + refresh tokens.
    - Save `RefreshToken`.
  - `login(email, password)`:
    - Look up `UserCredential`.
    - Check password (`passwordEncoder.matches`).
    - If ok, generate new access + refresh tokens; delete old refresh tokens for that user.
  - `refresh(refreshToken)`:
    - Look up refresh token in DB.
    - Check not expired.
    - Delete old one, issue a new pair.

**Why BCrypt?**

- Passwords must never be stored in plain text.
- BCrypt is a standard password hashing function supported by Spring Security.

### 4.4 Controller

- `AuthController` exposes:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
  - `POST /api/auth/refresh`
- Returns simple JSON with:
  - `accessToken`
  - `refreshToken`
  - `tokenType` = `Bearer`

**Why no Spring Security login endpoint?**

- For learning, a custom, small JSON API is clearer.
- Spring Security is configured to basically allow all requests and we **own** the auth API.

---

## 5. Example business services

Each service follows the same basic pattern:

1. **Main application** class with `@SpringBootApplication` + `@EnableDiscoveryClient`.
2. `application.yml`:
   - `spring.application.name`: unique service name.
   - `spring.datasource` / `spring.data.mongodb` for DB connection.
   - `eureka.client.service-url.defaultZone` pointing to Eureka.
3. One **entity/document**, **repository**, **service**, **controller**.

### 5.1 User Service

- Entity: `UserProfile`  
  - `authId` (points to `UserCredential` in auth DB by id).  
  - `fullName`, `phone`, `address`.
- Controller: `/api/users/me`  
  - Uses `@RequestHeader("X-User-Id") Long userId`.  
  - Reads or updates the profile record.

**Why `authId` instead of email?**

- Numeric id from auth DB is stable even if the user changes email.

### 5.2 Product, Order, Inventory, Payment

Each has:

- A JPA entity (e.g. `Product`, `Order`, `Stock`, `Payment`).  
- A repository interface (`extends JpaRepository`).  
- A simple service layer for CRUD.  
- REST controller with basic endpoints (`GET`, `POST`, etc.).

**Why separate DBs per service?**

- Enforces loose coupling ? no cross?service JOINs.
- Each service can evolve its schema independently.

### 5.3 Cart Service with MongoDB

- Document model: `Cart` with `userId` and list of `CartItem` records.  
- Repository: Spring Data Mongo `MongoRepository<Cart, String>`.  
- Service:
  - `getOrCreateCart(userId)` ? ensures cart exists.  
  - `addItem(...)` ? adds or increments quantity.  
  - `clearCart(userId)` ? empties cart.  
- Controller: `/api/cart` routes using `X-User-Id`.

**Why Mongo for cart?**

- Cart is a naturally nested document (list of items).
- Writes are simple (replace whole document) and we avoid joins.

---

## 6. Maven & project?local setup

You couldn?t install Maven globally, so we:

- Downloaded `apache-maven-3.9.12` into a `tools/` folder **inside the project**.
- Set environment variables **per terminal**:

  - `M2_HOME` ? absolute path to that folder.  
  - Prepend `M2_HOME\bin` to `Path`.

This lets you run `mvn` only inside project terminals, without touching system?wide settings.

We also created a small PowerShell script (e.g. `set-maven.ps1`) so each time you:

```powershell
cd "E-Shopping Case Study"
.\set-maven.ps1
```

and then run `mvn spring-boot:run` in any service.

---

## 7. How to practice / what to explore next

- **Trace a request**: start Eureka + Gateway + Auth + User, then:
  1. `POST /api/auth/register` ? get tokens.  
  2. `GET /api/users/me` with `Authorization: Bearer <accessToken>`.  
  3. Watch headers in a debugger to see `X-User-Id` flow through.
- **Break and fix**:
  - Change the `jwt.secret` in gateway but not in auth ? observe auth still issuing tokens but gateway rejecting them.
- **Extend**:
  - Add a new service (e.g. `review-service`) by copying the structure from `product-service`.  
  - Register it in Eureka and add a route in gateway.

If you understand each section above, you understand the core ideas behind **service discovery**, **API gateways**, **JWT auth**, and **per?service databases** in a Spring microservices system.

