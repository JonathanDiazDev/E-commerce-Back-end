# 🛒 E-commerce Backend API

> A production-ready RESTful API for e-commerce operations — built with Spring Boot 3, Spring Security, JWT authentication, and PostgreSQL.

[![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=java&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-336791?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/JWT-JJWT_0.12.6-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)](https://github.com/jwtk/jjwt)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

---

## 📌 Overview

<<<<<<< HEAD
This project is the backend of a full-featured e-commerce platform. It exposes a secure REST API currently focused on a complete authentication system, with product and cart modules actively under development. Designed with a layered architecture, it prioritizes clean code, security best practices, and scalability.
=======
This project is the backend of a full-featured e-commerce platform. It exposes a secure REST API that handles user authentication, product management, and shopping cart logic. Designed with a layered architecture, it focuses on clean code, security best practices, and scalability.
>>>>>>> feature/redis-management

---

## ✨ Features

<<<<<<< HEAD
- **JWT Authentication** — Stateless auth with access & refresh tokens using JJWT 0.12.6
- **Token Revocation** — Invalidation via a `tokens` table with `expired` and `revoked` flags
- **Refresh Token** — Silent session renewal without re-login via a dedicated endpoint
- **Logout & Logout All** — Single-device and all-devices session termination
- **Spring Security** — Role-based access control protecting all sensitive endpoints
=======
- **JWT Authentication** — Stateless auth with access tokens using JJWT 0.12.6, including token invalidation via a `tokens` table with `expired` and `revoked` flags
- **Spring Security** — Role-based access control protecting all sensitive endpoints
- **Product Management** — Full CRUD operations for product catalog and inventory
- **Shopping Cart** — Cart creation, item management, and quantity control per user
>>>>>>> feature/redis-management
- **Input Validation** — Request validation with Jakarta Bean Validation + Hibernate Validator
- **Layered Architecture** — Controller → Service → Repository separation of concerns
- **Lombok** — Reduced boilerplate across all entity and DTO classes

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.x |
| Security | Spring Security + JJWT 0.12.6 |
| ORM | Spring Data JPA (Hibernate) |
| Database | PostgreSQL |
| Validation | Jakarta Validation API + Hibernate Validator 8 |
| Utilities | Lombok, Google Guava 33 |
| Build Tool | Maven (Maven Wrapper included) |

---

## 📁 Project Structure

```
src/
└── main/
    ├── java/com/jonathan/ecommerce/
    │   ├── auth/           # Authentication controllers & DTOs
    │   ├── config/         # Security config, JWT filter, beans
    │   ├── model/          # JPA entities (User, Product, Cart, Token...)
    │   ├── repository/     # Spring Data JPA repositories
    │   ├── service/        # Business logic layer
    │   └── controller/     # REST controllers
    └── resources/
        └── application.properties
```

---

## 🔐 Security Design

<<<<<<< HEAD
Authentication is handled via **stateless JWT tokens**. Upon login, the server issues both an **access token** (short-lived) and a **refresh token** (long-lived). The refresh token allows the client to obtain a new access token without re-authenticating.
=======
Authentication is handled via **stateless JWT tokens**. Upon login, the server issues a signed JWT that must be included in the `Authorization` header of every protected request.
>>>>>>> feature/redis-management

Token invalidation is handled through a `tokens` table in the database:

```
tokens
├── id
├── token (String)
├── expired (boolean)
├── revoked (boolean)
└── user_id (FK)
```

<<<<<<< HEAD
On logout, the token is marked as `expired = true` and `revoked = true`, preventing reuse even if the JWT itself has not yet expired. The `logout-all` endpoint revokes every active token associated with the user across all devices.
=======
On logout, the token is marked as `expired = true` and `revoked = true`, preventing reuse even if the JWT itself has not yet expired.
>>>>>>> feature/redis-management

The `JwtAuthFilter` intercepts every request, validates the token signature, and checks the database to ensure the token has not been revoked.

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- PostgreSQL 15+ running locally or via Docker
- Maven (or use the included `./mvnw` wrapper)

### 1. Clone the repository

```bash
git clone https://github.com/JonathanDiazDev/E-commerce-Back-end.git
cd E-commerce-Back-end
```

### 2. Configure the database

Create a PostgreSQL database:

```sql
CREATE DATABASE ecommerce_db;
```

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce_db
spring.datasource.username=your_username
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

app.jwt.secret=your_super_secret_key_here
app.jwt.expiration=86400000
<<<<<<< HEAD
app.jwt.refresh-expiration=604800000
=======
>>>>>>> feature/redis-management
```

### 3. Run the application

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

---

## 📡 API Endpoints

### Auth

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/auth/register` | Register a new user | ❌ |
<<<<<<< HEAD
| `POST` | `/api/auth/login` | Authenticate — returns access + refresh token | ❌ |
| `POST` | `/api/auth/refresh-token` | Issue a new access token using the refresh token | ✅ |
| `POST` | `/api/auth/logout` | Invalidate the current session token | ✅ |
| `POST` | `/api/auth/logout-all` | Invalidate all active tokens for the user | ✅ |

> 📝 All protected endpoints require the header: `Authorization: Bearer <token>`

> ⚙️ **Product** and **Cart** endpoints are under active development.

=======
| `POST` | `/api/auth/login` | Authenticate and receive JWT | ❌ |
| `POST` | `/api/auth/logout` | Invalidate current token | ✅ |

### Products

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/api/products` | List all products | ❌ |
| `GET` | `/api/products/{id}` | Get product by ID | ❌ |
| `POST` | `/api/products` | Create a new product | ✅ ADMIN |
| `PUT` | `/api/products/{id}` | Update a product | ✅ ADMIN |
| `DELETE` | `/api/products/{id}` | Delete a product | ✅ ADMIN |

### Cart

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/api/cart` | Get current user's cart | ✅ |
| `POST` | `/api/cart/items` | Add item to cart | ✅ |
| `PUT` | `/api/cart/items/{id}` | Update item quantity | ✅ |
| `DELETE` | `/api/cart/items/{id}` | Remove item from cart | ✅ |

> 📝 All protected endpoints require the header: `Authorization: Bearer <token>`

>>>>>>> feature/redis-management
---

## 🧪 Testing the API

You can test the endpoints using [Postman](https://www.postman.com/) or any REST client.

**Example — Register a user:**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jonathan",
    "email": "jonathan@example.com",
    "password": "securePassword123"
  }'
```

**Example — Authenticated request:**

```bash
<<<<<<< HEAD
curl -X POST http://localhost:8080/api/auth/logout \
=======
curl -X GET http://localhost:8080/api/cart \
>>>>>>> feature/redis-management
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## 🔄 Roadmap

<<<<<<< HEAD
- [x] JWT authentication (access + refresh tokens)
- [x] Token revocation — logout & logout-all
- [x] Spring Security configuration & JWT filter
- [x] User registration & login
- [ ] Product CRUD *(entities defined, endpoints in progress)*
- [ ] Shopping cart logic *(entities defined, endpoints in progress)*
=======
- [x] JWT authentication with token revocation
- [x] Product CRUD
- [x] Shopping cart logic
>>>>>>> feature/redis-management
- [ ] Order management & checkout flow
- [ ] Docker Compose setup
- [ ] Swagger / OpenAPI documentation
- [ ] Unit & integration tests

---

## 👤 Author

**Jonathan Díaz**
Backend Developer — Java · Spring Boot · PostgreSQL

[![LinkedIn](https://img.shields.io/badge/LinkedIn-jonathan--diaz--backend-0A66C2?style=flat-square&logo=linkedin)](https://linkedin.com/in/jonathan-diaz-backend)
[![GitHub](https://img.shields.io/badge/GitHub-JonathanDiazDev-181717?style=flat-square&logo=github)](https://github.com/JonathanDiazDev)

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
