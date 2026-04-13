# 🛒 E-Commerce Backend

A production-ready RESTful API for an e-commerce platform built with **Java 21** and **Spring Boot 3**, focused on security, clean architecture, and scalability.

---

## 🚀 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Security | Spring Security 6 + JWT |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Cache | Redis |
| Containerization | Docker |
| API Docs | Swagger / OpenAPI 3 |
| Build Tool | Maven |

---

## 🏗️ Architecture

The project follows a **layered clean architecture** with clear separation of concerns:

```
controller/     → HTTP layer, request/response handling
service/        → Business logic (interface + impl pattern)
repository/     → Data access layer (Spring Data JPA)
entity/         → JPA entities
dto/            → Request and Response records
config/         → Security, Redis, and filter configuration
util/           → Shared utilities (e.g. token hashing)
```

---

## 🔐 Authentication & Security

- **JWT-based authentication** with separate access tokens (short-lived) and refresh tokens (long-lived)
- **Token whitelist** — access tokens are tracked in a `tokens` table with `expired` and `revoked` fields
- **Refresh token rotation** — single-use refresh tokens stored as SHA-256 hashes; each refresh invalidates the previous token
- **Redis blacklist** — revoked access tokens are cached in Redis for fast validation without DB hits
- **Session revocation** — `POST /auth/logout` revokes the current session; `POST /auth/logout-all` revokes all active sessions for a user
- **Role-Based Access Control (RBAC)** — `USER` and `ADMIN` roles with endpoint-level authorization
- **Stateless sessions** — no server-side session state; fully JWT-driven

---

## 📦 Modules

### ✅ Auth Module (Complete)
- `POST /api/v1/auth/register` — user registration
- `POST /api/v1/auth/login` — login with access + refresh token response
- `POST /api/v1/auth/refresh` — rotate refresh token
- `POST /api/v1/auth/logout` — revoke current session
- `POST /api/v1/auth/logout-all` — revoke all sessions

### ✅ Category Module (Complete)
- Hierarchical categories with recursive parent/child relationship
- Soft delete pattern — categories are deactivated, never hard deleted
- `GET /api/v2/categories` — all active categories
- `GET /api/v2/categories/{id}` — category by ID
- `GET /api/v2/categories/root-categories` — top-level categories only
- `POST /api/v2/categories/create` — create category (with optional parent)
- `DELETE /api/v2/categories/{id}` — soft deactivate category

### 🔄 In Progress
- `Product` — catalog management with category and inventory relations
- `Inventory` — stock tracking with movement history
- `Cart` + `CartItem` — shopping cart logic with stock validation
- `Payment` — polymorphic payment processing (Card, PayPal, Transfer)
- `Order` — created only upon payment confirmation

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

## ⚙️ Running Locally

### Prerequisites
- Java 21
- Docker & Docker Compose

### Steps

```bash
# Clone the repository
git clone https://github.com/JonathanDiazDev/E-commerce-Back-end.git
cd E-commerce-Back-end

# Start PostgreSQL and Redis with Docker
docker-compose up -d

# Run the application
./mvnw spring-boot:run
```

### API Documentation
Once running, Swagger UI is available at:
```
http://localhost:8080/swagger-ui/index.html
```

---

## 🔒 Security Design Decisions

**Why store refresh tokens as hashes?**
Storing raw tokens in the database is a security risk — if the database is compromised, all sessions are exposed. SHA-256 hashing ensures that even with DB access, tokens cannot be reused.

**Why Redis for the blacklist?**
Checking the DB on every request adds latency. Redis provides O(1) lookup with automatic TTL expiration matching the token's remaining lifetime.

**Why soft delete for categories?**
Hard deleting a category with associated products would create orphaned records. Soft delete preserves data integrity and allows reactivation without data loss.

---

## 👨‍💻 Author

**Jonathan Gabriel Díaz Gutiérrez**
Backend Developer — Java & Spring Boot

[![LinkedIn](https://img.shields.io/badge/LinkedIn-jonathan--diaz--backend-blue?logo=linkedin)](https://linkedin.com/in/jonathan-diaz-backend)
[![GitHub](https://img.shields.io/badge/GitHub-JonathanDiazDev-black?logo=github)](https://github.com/JonathanDiazDev)
