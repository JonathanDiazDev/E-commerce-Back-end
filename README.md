# 🛒 E-Commerce Backend API

> A production-grade RESTful API for e-commerce operations — built with Java 21, Spring Boot 3, Kafka, Redis, Stripe, and PostgreSQL.

[![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Apache_Kafka-Event_Streaming-231F20?style=flat-square&logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-336791?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-Cache_&_Rate_Limiting-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io/)
[![Stripe](https://img.shields.io/badge/Stripe-Payments-635BFF?style=flat-square&logo=stripe&logoColor=white)](https://stripe.com/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![CI](https://github.com/JonathanDiazDev/E-commerce-Back-end/actions/workflows/ci.yml/badge.svg)](https://github.com/JonathanDiazDev/E-commerce-Back-end/actions)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

---

## 📌 Overview

This project is the backend of a full-featured e-commerce platform. It exposes a secure REST API that handles user authentication, product catalog, inventory tracking, shopping cart, order placement, and payment processing via Stripe.

Designed with a layered clean architecture, it focuses on security best practices, event-driven reliability, and production-level patterns: Outbox pattern, DLQ-based retry, three-layer rate limiting, async processing, and structured observability through trace IDs.

---

## ✨ Key Features

### 🔐 Security & Authentication
- **JWT Authentication** — Stateless auth with short-lived access tokens and long-lived refresh tokens (JJWT 0.12.6)
- **Token Whitelist** — Access tokens tracked in DB with `expired` and `revoked` flags for precise session control
- **Refresh Token Rotation** — Single-use refresh tokens stored as SHA-256 hashes; each refresh invalidates the previous one
- **Redis Blacklist** — Revoked tokens cached in Redis for O(1) lookup, no DB hit per request
- **Session Revocation** — Logout current session or all active sessions simultaneously
- **Role-Based Access Control** — Spring Security 6 RBAC protecting sensitive endpoints

### 🛍️ Core Domain
- **Product Catalog** — Full CRUD with category and inventory linkage
- **Hierarchical Categories** — Parent/child structure with soft delete to preserve data integrity
- **Inventory Management** — Stock tracking with full movement history, paginated and sortable
- **Shopping Cart** — Add, update, remove items; pessimistic locking to prevent race conditions
- **Order Management** — Order placement tied to payment confirmation; full order history per user

### 💳 Payments & Reliability
- **Stripe Integration** — Payment processing with Stripe PaymentIntents and webhook signature validation
- **Refunds** — Stripe-backed refund processing via dedicated endpoint
- **Outbox Pattern** — Order events written atomically with DB state; polled by scheduler for guaranteed delivery
- **Payment Retry with DLQ** — Failed payment events retried via Kafka; dead-letter queue for unrecoverable failures

### ⚡ Performance & Resilience
- **Three-Layer Rate Limiting** — Bucket4j + Redis: global endpoint limits, per-user limits, and per-IP limits
- **Async Processing** — `@Async` with configurable thread pool for non-blocking event handling
- **Redis Caching** — Session blacklist and rate limit buckets stored in Redis
- **Database Connection Warmup** — Eager pool initialization on startup to prevent cold-start latency

### 🔍 Observability
- **Trace ID Filter** — Unique `X-Trace-Id` propagated through every request for end-to-end log correlation
- **Structured Logging** — Contextual logging across all service layers with SLF4J + Logback
- **Email Notifications** — Kafka-driven transactional emails via Spring Mail + Thymeleaf templates; failed emails persisted for retry

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security 6 + JJWT 0.12.6 |
| ORM | Spring Data JPA (Hibernate) |
| Database | PostgreSQL 16 |
| Cache | Redis (Lettuce client) |
| Message Broker | Apache Kafka (Confluent 7.4) |
| Payments | Stripe Java SDK 25.6 |
| Rate Limiting | Bucket4j 8.10 + Redis |
| Email | Spring Mail + Thymeleaf |
| Mapping | MapStruct + Lombok |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Retry | Spring Retry + Spring AOP |
| Migrations | Flyway |
| Testing | JUnit 5 + Mockito + Spring Boot Test |
| Build | Maven (wrapper included) |
| Infrastructure | Docker + Docker Compose |
| CI/CD | GitHub Actions |

---

## 📁 Project Structure

```
src/main/java/com/jonathan/ecommerce/
├── config/                  # Security, JWT filter, Redis, Async, Stripe configs
├── controller/              # REST controllers (Auth, Product, Cart, Order, Payment...)
├── dto/
│   ├── request/             # Incoming request records
│   ├── response/            # Outgoing response records
│   ├── enums/               # DTO-level enums (ErrorCode, SortDirection...)
│   ├── mapper/              # MapStruct mappers
│   └── webhook/             # Stripe webhook payload DTOs
├── entity/
│   └── enums/               # Domain enums (Role, OrderStatus, PaymentStatus...)
├── exception/               # Custom exceptions + GlobalExceptionHandler
├── kafka/
│   ├── config/              # Kafka topic and serializer config
│   ├── consumer/            # Order, Email, PaymentRetry, DLQ consumers
│   ├── producer/            # Order, Email, PaymentRetry producers
│   ├── processor/           # OutboxProcessor
│   └── scheduler/           # OutboxScheduler, EmailRetryScheduler
├── ratelimiting/
│   ├── annotation/          # @RateLimited custom annotation
│   ├── aspect/              # AOP-based rate limiting aspect
│   ├── config/              # Bucket4j + Redis proxy config
│   ├── exception/           # RateLimitExceededException
│   └── filter/              # Filter-based rate limiting (SecurityFilterChain)
├── repository/              # Spring Data JPA repositories
├── service/                 # Business logic interfaces
│   ├── impl/                # Service implementations
│   └── helper/              # Internal service helpers
└── util/                    # Shared utilities (HashUtil, RequestContextUtil...)
```

---

## 🔐 Security Design

Authentication is handled via **stateless JWT tokens**. Upon login, the server issues a signed access token (short-lived) and a single-use refresh token (long-lived).

**Why hash refresh tokens?**
Storing raw tokens in the database is a security risk — if the DB is compromised, all sessions are exposed. SHA-256 hashing ensures that even with DB access, tokens cannot be reused.

**Why Redis for the blacklist?**
Checking the DB on every request adds latency. Redis provides O(1) lookup with automatic TTL expiration matching the token's remaining lifetime.

---

## 📨 Event-Driven Architecture

Critical flows are decoupled from the HTTP request via Kafka and the Outbox pattern — guaranteeing **at-least-once delivery** without distributed transactions.

```
┌─────────────────────────────────────────────────────────────────┐
│                     OUTBOX PATTERN FLOWS                         │
│         (atomic DB write + guaranteed Kafka delivery)            │
└─────────────────────────────────────────────────────────────────┘

POST /auth/register
    │
    ▼
AuthService ──► saves User + OutboxEvent(USER_REGISTERED)
                                    │
                          OutboxScheduler polls (every 30s)
                                    │
                          UserRegisteredProducer ──► Kafka
                                                        │
                                          EmailKafkaConsumer ──► welcome email

POST /auth/password-reset
    │
    ▼
PasswordResetService ──► saves Token + OutboxEvent(PASSWORD_RESET_REQUESTED)
                                    │
                          OutboxScheduler polls (every 30s)
                                    │
                          PasswordResetProducer ──► Kafka
                                                        │
                                          EmailKafkaConsumer ──► reset email

POST /orders/checkout
    │
    ▼
OrderService ──► saves Order + OutboxEvent(ORDER_PLACED)
                                    │
                          OutboxScheduler polls (every 30s)
                                    │
                          OrderKafkaProducer ──► Kafka topic: order.placed
                                                        │
                                          OrderKafkaConsumer ──► inventory deduct
                                          EmailKafkaConsumer ──► confirmation email
                                                        │
                                          PaymentRetryProducer (on failure)
                                                        │
                                          PaymentRetryDlqConsumer (DLQ)

┌─────────────────────────────────────────────────────────────────┐
│                     DIRECT FLOWS (no Outbox)                     │
└─────────────────────────────────────────────────────────────────┘

Stock restock ──► ApplicationEvent ──► EmailKafkaConsumer ──► stock availability email
Failed emails ──► EmailRetryScheduler polls (every 2 min) ──► retry send
```

---

## ⚡ Rate Limiting Design

Three independent layers, applied in order on every request:

| Layer | Scope | Backend |
|---|---|---|
| Endpoint limit | Per route (global) | Redis Bucket4j |
| User limit | Per authenticated user | Redis Bucket4j |
| IP limit | Per source IP | Redis Bucket4j |

Configuration is centralized in `application.yml` — no annotations required on controllers. The filter runs inside the Spring Security chain, after JWT authentication.

---

## 🗂️ Entity Overview

```
User ──────── RefreshToken (hashed, single-use)
  │
  ├────────── Cart ──── CartItem ──── Product
  │                                      │
  └────────── Order ─── OrderItem ───────┤
                │                    Category (hierarchical)
                │                        │
              Payment              Inventory ── InventoryMovement
                │
         PaymentAttempt
```

---

## 📦 API Endpoints

### Auth
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|:---:|
| `POST` | `/api/v1/auth/register` | Register a new user | ❌ |
| `POST` | `/api/v1/auth/login` | Login — returns access + refresh token | ❌ |
| `POST` | `/api/v1/auth/refresh` | Rotate refresh token | ❌ |
| `POST` | `/api/v1/auth/logout` | Revoke current session | ✅ |
| `POST` | `/api/v1/auth/logout-all` | Revoke all active sessions | ✅ |

### Products
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|:---:|
| `GET` | `/api/v1/products` | All active products | ✅ |
| `GET` | `/api/v1/products/{id}` | Product by ID | ✅ |
| `POST` | `/api/v1/products/create` | Create product | ✅ ADMIN |
| `PUT` | `/api/v1/products/{id}` | Update product | ✅ ADMIN |
| `DELETE` | `/api/v1/products/{id}` | Soft delete product | ✅ ADMIN |

### Categories
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|:---:|
| `GET` | `/api/v2/categories` | All active categories | ✅ |
| `GET` | `/api/v2/categories/{id}` | Category by ID | ✅ |
| `GET` | `/api/v2/categories/root-categories` | Top-level categories | ✅ |
| `POST` | `/api/v2/categories/create` | Create category | ✅ ADMIN |
| `DELETE` | `/api/v2/categories/{id}` | Soft delete category | ✅ ADMIN |

### Inventory
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|:---:|
| `GET` | `/api/v1/inventories/{productId}` | Stock details | ✅ |
| `GET` | `/api/v1/inventories/product/{productId}/history` | Paginated movement history | ✅ |
| `POST` | `/api/v1/inventories/{productId}/add` | Add stock | ✅ ADMIN |
| `POST` | `/api/v1/inventories/{productId}/deduct` | Deduct stock | ✅ ADMIN |
| `PATCH` | `/api/v1/inventories/{productId}/status` | Update inventory status | ✅ ADMIN |

### Cart
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|:---:|
| `GET` | `/api/v1/carts` | Get current user's cart | ✅ |
| `POST` | `/api/v1/carts/item` | Add item to cart | ✅ |
| `PUT` | `/api/v1/carts/item/{cartItemId}` | Update item quantity | ✅ |
| `DELETE` | `/api/v1/carts/item/{cartItemId}` | Remove item from cart | ✅ |

### Orders
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|:---:|
| `POST` | `/api/v1/orders/checkout` | Place an order | ✅ |
| `GET` | `/api/v1/orders` | Get all orders for current user | ✅ |
| `GET` | `/api/v1/orders/{orderId}` | Get order by ID | ✅ |

### Payments
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|:---:|
| `POST` | `/api/v1/payments/process` | Process payment (Stripe) | ✅ |
| `POST` | `/api/v1/payments/webhook` | Stripe webhook receiver | ❌ (signature-verified) |
| `POST` | `/api/v1/refunds` | Process a refund | ✅ ADMIN |

---

## 🧪 Testing

Unit tests written with JUnit 5 + Mockito covering the core service layer:

| Test Class | Coverage |
|---|---|
| `AuthServiceTest` | Refresh token rotation, expiration, invalid token |
| `CartServiceTest` | Add item, product not found, insufficient stock, manual disabled |
| `InventoryServiceTest` | Stock details, deduct stock, insufficient stock, manual disabled |
| `PasswordResetServiceImplTest` | Token validation, expiration, not found |
| `ProductServiceTest` | Product CRUD operations |

```bash
# Run unit tests
./mvnw test "-Dtest=AuthServiceTest,CartServiceTest,InventoryServiceTest,PasswordResetServiceImplTest,ProductServiceTest"
```

> Integration tests with Testcontainers are in progress.

---

## 🚀 Getting Started

### Prerequisites

- Docker & Docker Compose
- A [Stripe](https://stripe.com) account (test mode keys are sufficient)

### 1. Clone the repository

```bash
git clone https://github.com/JonathanDiazDev/E-commerce-Back-end.git
cd E-commerce-Back-end
```

### 2. Configure environment variables

```bash
cp env.example .env
```

Edit `.env` and fill in your values:

```env
DB_PASSWORD=your_password
JWT_ACCESS_TOKEN=your-secret-key-at-least-32-characters
MAIL_USERNAME=your_mailtrap_username
MAIL_PASSWORD=your_mailtrap_password
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

### 3. Start all services

```bash
docker-compose up -d
```

This starts PostgreSQL, Redis, Kafka, Zookeeper, and the application itself. Flyway migrations run automatically on startup.

| URL | Description |
|-----|-------------|
| `http://localhost:8080` | API base URL |
| `http://localhost:8080/swagger-ui/index.html` | Interactive API docs |

---

## 🧪 Testing

Unit tests are written with JUnit 5 + Mockito covering the core service layer:

| Test Class | Coverage |
|---|---|
| `AuthServiceTest` | Refresh token rotation, expiration, invalid token |
| `CartServiceTest` | Add item, product not found, insufficient stock, manual disabled |
| `InventoryServiceTest` | Stock details, deduct stock, insufficient stock, manual disabled |
| `PasswordResetServiceImplTest` | Token validation, expiration, not found |
| `ProductServiceTest` | Product CRUD operations |

```bash
# Run unit tests
./mvnw test "-Dtest=AuthServiceTest,CartServiceTest,InventoryServiceTest,PasswordResetServiceImplTest,ProductServiceTest"
```

> Integration tests with Testcontainers are in progress.

---

## 🧪 Quick API Test

**Register a user:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jonathan",
    "email": "jonathan@example.com",
    "password": "SecurePass123!"
  }'
```

> ⚠️ Password requirements: 12–20 characters, at least one letter, one number, and one special character.

**Login:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "jonathan@example.com", "password": "SecurePass123!"}'
```

**Authenticated request:**

```bash
curl -X GET http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer <your_access_token>"
```

---

## ✅ Roadmap

- [x] JWT authentication with access + refresh tokens
- [x] Token whitelist and Redis blacklist
- [x] Refresh token rotation with SHA-256 hashing
- [x] Logout and logout-all session revocation
- [x] Role-based access control (RBAC)
- [x] Product catalog with category and inventory relations
- [x] Hierarchical category management with soft delete
- [x] Inventory tracking with paginated movement history
- [x] Shopping cart with pessimistic locking
- [x] Order placement and history
- [x] Stripe payment processing + webhook integration
- [x] Stripe refund support
- [x] Outbox pattern for reliable order event delivery
- [x] Kafka-driven email notifications with DLQ
- [x] Payment retry with dead-letter queue
- [x] Three-layer rate limiting (Bucket4j + Redis)
- [x] Async processing with configurable thread pool
- [x] Trace ID propagation for request correlation
- [x] Flyway database migrations
- [x] Docker Compose for full local infrastructure
- [x] CI/CD pipeline with GitHub Actions
- [ ] Integration test coverage with Testcontainers
- [ ] AWS deployment (ECS + RDS + ElastiCache)

---

## 🔒 Design Decisions

**Why soft delete for categories?**
Hard deleting a category with associated products would create orphaned records. Soft delete preserves data integrity and allows reactivation without data loss.

**Why the Outbox pattern?**
Writing an order and publishing a Kafka event in separate operations risks a partial failure — the order is saved but the event is lost, or vice versa. The Outbox pattern ensures both happen atomically within the same DB transaction, with the scheduler handling delivery separately.

**Why pessimistic locking on cart operations?**
Cart updates can trigger concurrent stock validation. Pessimistic locking at the DB level prevents race conditions where two requests simultaneously pass the stock check for the same inventory record.

**Why hash refresh tokens?**
Storing raw tokens is equivalent to storing plaintext passwords — a DB breach would expose all active sessions. SHA-256 hashing ensures the stored value is useless without the original token.

**Why the interface + impl pattern for services?**
It enforces a contract between layers, makes testing easier (mock the interface, not the implementation), and follows standard Spring Boot conventions.

---

## 🛠️ Development

### Workflow

```bash
# 1. Create a feature branch
git checkout -b feature/your-feature-name

# 2. Run tests
./mvnw clean test

# 3. Commit using conventional commits
git commit -m "feat: add stock notification endpoint"
git push origin feature/your-feature-name
```

### Running in debug mode

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

Then attach your IDE debugger to port `5005`.

### Implementation Notes

- **Monetary values** — `BigDecimal` throughout to avoid floating-point precision errors
- **Timestamps** — `Instant` (UTC) for all date/time fields
- **Lazy loading** — `FetchType.LAZY` on all JPA relations to prevent N+1 queries
- **Transaction isolation** — `@Transactional` on all services; `SERIALIZABLE` isolation where stock consistency is critical

---

## 🔧 Troubleshooting

### Kafka issues

```bash
# List all topics
docker exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# Check consumer group lag
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group ecommerce-group \
  --describe

# View messages in a topic
docker exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order.placed \
  --from-beginning
```

### Redis issues

```bash
# Connect to Redis CLI
docker exec -it redis redis-cli

# Check blacklisted tokens
KEYS blacklist:*
```

### View application logs

```bash
docker-compose logs -f app
```

---

## 👤 Author

**Jonathan Díaz**
Self-taught Java Backend Developer · Spring Boot · PostgreSQL · Kafka

[![LinkedIn](https://img.shields.io/badge/LinkedIn-jonathan--diaz--backend-0A66C2?style=flat-square&logo=linkedin)](https://linkedin.com/in/jonathan-diaz-backend)
[![GitHub](https://img.shields.io/badge/GitHub-JonathanDiazDev-181717?style=flat-square&logo=github)](https://github.com/JonathanDiazDev)

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
