# 🛒 E-commerce Backend API

> A production-ready RESTful API for e-commerce operations — built with Spring Boot 3, Spring Security 6, JWT authentication, Redis, and PostgreSQL.

[![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=java&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-336791?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-Cache-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io/)
[![JWT](https://img.shields.io/badge/JWT-JJWT_0.12.6-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)](https://github.com/jwtk/jjwt)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

---

## 📌 Overview

This project is the backend of a full-featured e-commerce platform. It exposes a secure REST API that handles user authentication, product management, category management, and shopping cart logic. Designed with a layered clean architecture, it focuses on clean code, security best practices, and scalability.

---

## ✨ Features

- **JWT Authentication** — Stateless auth with short-lived access tokens and long-lived refresh tokens using JJWT 0.12.6
- **Token Whitelist** — Access tokens tracked in a `tokens` table with `expired` and `revoked` flags for precise session control
- **Refresh Token Rotation** — Single-use refresh tokens stored as SHA-256 hashes; each refresh invalidates the previous token
- **Redis Blacklist** — Revoked access tokens cached in Redis for O(1) lookup without DB hits on every request
- **Session Revocation** — Logout current session or all active sessions simultaneously (`logout-all`)
- **Spring Security 6** — Role-based access control (RBAC) protecting all sensitive endpoints
- **Category Management** — Hierarchical categories with parent/child relationships and soft delete pattern
- **Shopping Cart** — Cart creation, item management, and stock validation per user
- **Input Validation** — Request validation with Jakarta Bean Validation + Hibernate Validator
- **Layered Architecture** — Controller → Service (interface + impl) → Repository separation of concerns
- **Lombok** — Reduced boilerplate across all entity and DTO classes

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.x |
| Security | Spring Security 6 + JJWT 0.12.6 |
| ORM | Spring Data JPA (Hibernate) |
| Database | PostgreSQL |
| Cache | Redis |
| Validation | Jakarta Validation API + Hibernate Validator 8 |
| Utilities | Lombok, Google Guava 33 |
| API Docs | Swagger / OpenAPI 3 |
| Build Tool | Maven (Maven Wrapper included) |

---

## 📁 Project Structure

```
src/
└── main/
    ├── java/com/jonathan/ecommerce/
    │   ├── config/         # Security config, JWT filter, Redis config
    │   ├── controller/     # REST controllers
    │   ├── dto/
    │   │   ├── request/    # Incoming request records
    │   │   └── response/   # Outgoing response records
    │   ├── entity/         # JPA entities (User, Product, Cart, Order...)
    │   │   └── enums/      # Domain enums (Role, OrderStatus, etc.)
    │   ├── repository/     # Spring Data JPA repositories
    │   ├── service/        # Business logic interfaces
    │   │   └── impl/       # Service implementations
    │   └── util/           # Shared utilities (HashUtil, etc.)
    └── resources/
        └── application.properties
```

---

## 🔐 Security Design

Authentication is handled via **stateless JWT tokens**. Upon login, the server issues a signed access token (short-lived) and a refresh token (long-lived, single-use).

**Why hash refresh tokens?**
Storing raw tokens in the database is a security risk — if the database is compromised, all sessions are exposed. SHA-256 hashing ensures that even with DB access, tokens cannot be reused.

**Why Redis for the blacklist?**
Checking the DB on every request adds latency. Redis provides O(1) lookup with automatic TTL expiration matching the token's remaining lifetime.

Token invalidation is handled through dedicated tables:

```
tokens
├── id
├── token        (String)
├── expired      (boolean)
├── revoked      (boolean)
└── user_id      (FK → users)

refresh_tokens
├── id
├── token_hash         (SHA-256 hash)
├── expires_at         (Instant)
├── revoked            (boolean)
├── replaced_by_token  (hash of next token)
└── user_id            (FK → users)
```

---

## 🗂️ Entity Overview

```
User ──────────── Token (access token whitelist)
  │
  └──────────────── RefreshToken (hashed, single-use rotation)
  │
  └──────────────── Order ──── OrderItem ──── Product
                      │
                    Cart ───── CartItem ───── Product
                                                │
                                           Category (hierarchical)
                                                │
                                           Inventory ── InventoryMovement
```

---

## 📦 Modules

### ✅ Auth (Complete)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/api/v1/auth/register` | Register a new user | ❌ |
| `POST` | `/api/v1/auth/login` | Login with access + refresh token | ❌ |
| `POST` | `/api/v1/auth/refresh` | Rotate refresh token | ❌ |
| `POST` | `/api/v1/auth/logout` | Revoke current session | ✅ |
| `POST` | `/api/v1/auth/logout-all` | Revoke all active sessions | ✅ |

### ✅ Category (Complete)
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/api/v2/categories` | All active categories | ✅ |
| `GET` | `/api/v2/categories/{id}` | Category by ID | ✅ |
| `GET` | `/api/v2/categories/root-categories` | Top-level categories | ✅ |
| `POST` | `/api/v2/categories/create` | Create category | ✅ |
| `DELETE` | `/api/v2/categories/{id}` | Soft deactivate category | ✅ ADMIN |

### 🔄 In Progress
- `Product` — catalog management with category and inventory relations
- `Inventory` — stock tracking with movement history
- `Cart` + `CartItem` — shopping cart logic with stock validation
- `Payment` — polymorphic payment processing (Card, PayPal, Transfer)
- `Order` — created only upon payment confirmation

---

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven (or use the included `./mvnw` wrapper)

### 1. Clone the repository

```bash
git clone https://github.com/JonathanDiazDev/E-commerce-Back-end.git
cd E-commerce-Back-end
```

### 2. Start PostgreSQL and Redis

```bash
docker-compose up -d
```

### 3. Configure environment variables

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce_db
spring.datasource.username=your_username
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update

app.jwt.secret=your_super_secret_key_here
app.jwt.expiration=900000
app.jwt.refresh-expiration=604800000
```

### 4. Run the application

```bash
./mvnw spring-boot:run
```

API available at `http://localhost:8080`
Swagger UI at `http://localhost:8080/swagger-ui/index.html`

---

## 🧪 Testing the API

**Register a user:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jonathan",
    "email": "jonathan@example.com",
    "password": "securePassword123"
  }'
```

**Authenticated request:**

```bash
curl -X GET http://localhost:8080/api/v2/categories \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

> 📝 All protected endpoints require the header: `Authorization: Bearer <token>`

---

## 🔄 Roadmap

- [x] JWT authentication with access + refresh tokens
- [x] Token whitelist and Redis blacklist
- [x] Refresh token rotation with SHA-256 hashing
- [x] Logout and logout-all session revocation
- [x] Role-based access control (RBAC)
- [x] Hierarchical category management with soft delete
- [ ] Product CRUD with inventory tracking
- [ ] Shopping cart logic with stock validation
- [ ] Polymorphic payment processing
- [ ] Order management and checkout flow
- [ ] Flyway database migrations
- [ ] Unit and integration tests
- [ ] Full Docker Compose setup

---

## 🔒 Additional Design Decisions

**Why soft delete for categories?**
Hard deleting a category with associated products would create orphaned records. Soft delete preserves data integrity and allows reactivation without data loss.

**Why the interface + impl pattern for services?**
It enforces a contract between layers, makes the codebase easier to test (mock the interface, not the implementation), and follows industry-standard Spring Boot conventions.

---

## 👤 Author

**Jonathan Díaz**
Backend Developer — Java · Spring Boot · PostgreSQL

[![LinkedIn](https://img.shields.io/badge/LinkedIn-jonathan--diaz--backend-0A66C2?style=flat-square&logo=linkedin)](https://linkedin.com/in/jonathan-diaz-backend)
[![GitHub](https://img.shields.io/badge/GitHub-JonathanDiazDev-181717?style=flat-square&logo=github)](https://github.com/JonathanDiazDev)

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
