
<div align="center">

# 🛒 E-Commerce Backend

**Arquitectura moderna de backend transaccional para comercio electrónico**  
Construido con **Java 21**, **Spring Boot 3.5** y una infraestructura basada en **eventos**, diseñado para escalar con consistencia y resiliencia.

[![Java](https://img.shields.io/badge/Java-21-%23ED8B00?logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-%236DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-%234169E1?logo=postgresql)](https://www.postgresql.org/)
[![Apache Kafka](https://img.shields.io/badge/Kafka-7.4-%23231F20?logo=apachekafka)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-7-%23DC382D?logo=redis)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-%232496ED?logo=docker)](https://www.docker.com/)
[![Stripe](https://img.shields.io/badge/Stripe-Payments-%23635BFF?logo=stripe)](https://stripe.com/)
[![Tests](https://img.shields.io/badge/Tests-252_passing-%2328a745)](#)

</div>

---

## 📋 Tabla de Contenidos

- [Descripción General](#-descripción-general)
- [Características Clave](#-características-clave)
- [Arquitectura y Patrones de Diseño](#-arquitectura-y-patrones-de-diseño)
- [Stack Tecnológico](#-stack-tecnológico)
- [Modelo de Datos](#-modelo-de-datos)
- [Estrategia de Pruebas](#-estrategia-de-pruebas)
- [Primeros Pasos](#-primeros-pasos)
- [Estructura del Proyecto](#-estructura-del-proyecto)

---

## 🚀 Descripción General

Backend completo para una plataforma de e-commerce que expone una API RESTful segura y escalable. Gestiona el ciclo de vida completo de una orden transaccional: desde el registro de usuarios autenticados con **JWT + Refresh Tokens**, pasando por la gestión de carrito de compras con bloqueo pesimista de inventario, hasta el procesamiento de pagos con **Stripe** con reintentos automáticos vía **Kafka**.

El sistema garantiza consistencia eventual entre los servicios a través del **Outbox Pattern**, combinado con un sistema de **Rate Limiting** de tres capas (endpoint, usuario e IP) implementado con **Bucket4j** sobre **Redis** para proteger los endpoints críticos contra abusos.

---

## ✨ Características Clave

### Autenticación y Seguridad
- **Registro y login** con hashing de contraseñas (BCrypt)
- **JWT bidireccional**: Access Token (corto) + Refresh Token (duradero) rotados en cada uso
- **Sesiones múltiples** con detección de dispositivos (IP + User-Agent)
- **Cierre de sesión** individual o global (invalida todos los refresh tokens)
- **Cambio de contraseña** seguro con verificación de contraseña actual
- Endpoints públicos protegidos con **Rate Limiting** asimétrico por ruta

### Catálogo y Productos
- CRUD completo de productos con categorías
- Búsqueda por nombre, categoría y rango de precios
- Inventario por producto con stock atómico y bloqueo pesimista (`PESSIMISTIC_WRITE`)
- Desactivación lógica de productos y deshabilitación manual de inventario

### Carrito de Compras
- Creación automática de carrito por usuario (uno activo por usuario)
- Agregar/actualizar/remover items con control de stock en tiempo real
- Cálculo de totales en tiempo real
- Limpieza completa del carrito con devolución de stock

### Órdenes y Pagos
- **Checkout transaccional**: descuenta inventario con bloqueo pesimista, crea la orden y procesa el pago en una sola transacción
- Integración con **Stripe Payment Intents** para procesamiento de pagos
- Manejo de webhooks de Stripe para confirmación asíncrona de pagos
- **Sistema de reintentos**: si el pago falla, se encola un evento en Kafka con backoff progresivo (5s, 10s, 30s) hasta 3 intentos
- Pagos fallidos almacenados en tabla `failed_payment_event` con alertas al administrador

### Event-Driven Architecture (Kafka + Outbox Pattern)
| Topic | Propósito |
|---|---|
| `order-emails-topic` | Notificaciones de órdenes colocadas |
| `stock-emails-topic` | Alertas de disponibilidad de stock |
| `payment-retry-topic` | Reintentos de pago fallidos |
| `password-reset-topic` | Enlaces de restablecimiento de contraseña |
| `user-registered-topic` | Bienvenida a nuevos usuarios |

El **Outbox Pattern** garantiza que cada evento se persista en la BD en la misma transacción que la operación de negocio, y un scheduler los publica en Kafka de forma atómica y confiable.

### Rate Limiting (Bucket4j + Redis)
Protección de tres capas configurable por endpoint:

| Endpoint | Global | Por Usuario | Por IP |
|---|---|---|---|
| `/api/v1/auth/login` | 5 req/min | 10 req/min | 50 req/min |
| `/api/v1/auth/register` | 3 req/5min | — | 20 req/5min |
| `/api/v1/order/checkout` | 20 req/min | 10 req/min | 100 req/min |
| `/api/v1/cart/**` | 200 req/min | 100 req/min | 500 req/5min |
| `/api/v1/products/**` | 500 req/min | 300 req/min | 1000 req/5min |

Soporta **Fail-Open**: si Redis no está disponible, permite el paso (configurable por endpoint).

### Notificaciones por Email
- Email transaccional con **Thymeleaf** como motor de plantillas
- Alertas de disponibilidad de stock (stock bajo → notificación)
- Confirmación de registro y órdenes
- Enlaces de recuperación de contraseña
- Alertas administrativas para fallos de pago
- Reintentos programados para emails fallidos (`EmailRetryScheduler`)

---

## 🏗️ Arquitectura y Patrones de Diseño

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway                              │
│                    RateLimitingFilter                            │
├─────────────────────────────────────────────────────────────────┤
│                    Security Layer                                │
│         JwtAuthenticationFilter → SecurityContextHolder          │
├─────────────────────────────────────────────────────────────────┤
│                       Controllers                                │
│   Auth  Cart  Products  Orders  Payments  Categories  Inventory  │
├─────────────────────────────────────────────────────────────────┤
│                     Service Layer                                │
│   AuthServiceImpl  CartServiceImpl  OrderServiceImpl   ...      │
├─────────────────────────────────────────────────────────────────┤
│          Repository Layer (Spring Data JPA)                      │
├─────────────────────────────────────────────────────────────────┤
│      ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│      │  PostgreSQL   │  │    Redis     │  │  Apache Kafka │       │
│      │   (Datos)     │  │  (Caché + RL)│  │  (Eventos)    │       │
│      └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

### Patrones Implementados

| Patrón | Implementación |
|---|---|
| **Arquitectura en Capas** | Controller → Service → Repository, con DTOs para desacoplar la API del dominio |
| **Outbox Pattern** | `OutboxEvent` se persiste en la misma transacción que la operación de negocio; `OutboxScheduler` procesa los eventos pendientes cada 30s y los publica en Kafka |
| **Event-Driven Architecture** | 5 topics de Kafka para desacoplar el flujo principal (órdenes, pagos, emails) de los procesos secundarios |
| **Rate Limiting (Token Bucket)** | Tres capas (endpoint, usuario, IP) usando Bucket4j distribuido sobre Redis |
| **Transactional Outbox** | Garantiza que cada evento se persista atómicamente con su operación de negocio |
| **Pessimistic Locking** | `findWithLockByProductId` con `PESSIMISTIC_WRITE` para evitar condiciones de carrera en inventario |
| **Retry Pattern** | Payment retry con backoff progresivo (5s → 10s → 30s) y DLQ para fallos definitivos |
| **DTO Pattern** | `request/` y `response/` separados de las entidades JPA para control total sobre la serialización |
| **MapStruct** | Mapeo automático entre entidades y DTOs con configuración `componentModel = spring` |
| **Strategy Pattern** | Diferentes estrategias de pago (Stripe Payment Intents, Webhooks) |
| **Null Object / Fail-Open** | Rate Limiting desactivado cuando Redis no está disponible, configurable por endpoint |

---

## 🛠️ Stack Tecnológico

| Categoría | Tecnología | Versión |
|---|---|---|
| **Lenguaje** | Java | 21 (LTS) |
| **Framework** | Spring Boot | 3.5.13 |
| **Seguridad** | Spring Security + JWT (jjwt 0.13) | — |
| **Base de Datos** | PostgreSQL | 16 |
| **ORM** | Hibernate / Spring Data JPA | — |
| **Migraciones** | Flyway | — |
| **Mensajería** | Apache Kafka (vía Spring Kafka) | 7.4 |
| **Caché / Distribuido** | Redis + Lettuce | 7 |
| **Rate Limiting** | Bucket4j (core + lettuce) | 8.14 |
| **Pagos** | Stripe SDK | 26.6 |
| **Mapper** | MapStruct (+ Lombok binding) | 1.6.3 |
| **Utilidades** | Lombok, Google Guava, Gson | — |
| **Documentación API** | SpringDoc OpenAPI (Swagger UI) | 2.8.5 |
| **Email** | Spring Mail + Thymeleaf (templates) | — |
| **Containerización** | Docker + Docker Compose | — |
| **Construcción** | Maven (wrapper incluido) | — |
| **Pruebas** | JUnit 5, Mockito, AssertJ, Spring Security Test, Testcontainers (PostgreSQL + Kafka) | — |

---

## 💾 Modelo de Datos

### Entidades Principales

```
User (1) ──── (N) Order
  │                 │
  │                 ├── (N) OrderItem ──── Product
  │                 │
  │                 └── (1) Payment ──── (N) PaymentAttempt
  │
  ├── (1) Cart ──── (N) CartItem ──── Product
  │
  ├── (N) RefreshToken
  │
  └── (N) PasswordResetToken

Product (1) ──── (1) Inventory
  │                       │
  └── (N) Category        └── (N) InventoryMovement

OutboxEvent ───→ Kafka Topics
ProcessedEvent ── (idempotencia Kafka)
FailedEmail ──── (reintentos de email)
FailedPaymentEvent (pagos fallidos definitivos)
StockNotification (alertas de stock bajo)
```

### Outbox Pattern — Flujo de Consistencia

```
Operación de Negocio (Transacción DB)
         │
         ├── INSERT en tabla principal (order, user, etc.)
         └── INSERT en outbox_event (misma transacción)
                     │
                     ▼
         OutboxScheduler (@Scheduled 30s)
                     │
                     ▼
         OutboxProcessor.processSingleEvent()
                     │
              ┌──────┴──────┐
              ▼              ▼
         Kafka Topic    outbox_status = SENT / FAILED
              │
              ▼
         EmailKafkaConsumer
              │
         (idempotencia vía ProcessedEvent)
```

---

## 🧪 Estrategia de Pruebas

El proyecto cuenta con **252 pruebas automatizadas** distribuidas en dos categorías principales, todas ejecutables localmente con Docker:

### Pruebas Unitarias (`@ExtendWith(MockitoExtension.class)`)
- **~135 tests** que cubren servicios, controladores, consumidores Kafka, filtros de seguridad y aspectos de rate limiting
- Mock completo de todas las dependencias externas (repositorios Spring Data, productores Kafka, servicios de email)
- Validación de excepciones, flujos alternos, idempotencia y casos borde
- Controladores probados con `@WebMvcTest` + `@MockitoBean` + `@WithMockUser`
- Consumidores Kafka probados de forma aislada con Mockito (`@Mock` + `@InjectMocks`)
- No requieren Docker ni ninguna infraestructura externa

### Pruebas de Integración (`@SpringBootTest` + `@Transactional` + Testcontainers)
- **~22 tests** que levantan el contexto completo de Spring sobre una base de datos **PostgreSQL 16 real** provisionada por **Testcontainers**
- Cada ejecución de tests arranca un contenedor PostgreSQL dinámico y efímero (puerto aleatorio), lo que garantiza aislamiento total entre ejecuciones y elimina la necesidad de una base de datos local dedicada
- `BaseIntegrationTest` expone `@DynamicPropertySource` que inyecta la URL, usuario y contraseña del contenedor PostgreSQL en Spring, reemplazando cualquier configuración estática
- Perfil `test` (`@ActiveProfiles("test")` + `application-test.yml`) que:
  - Deshabilita Redis, Kafka y Flyway mediante `spring.autoconfigure.exclude`
  - Aísla productores Kafka (`OrderKafkaProducer`, `PasswordResetKafkaProducer`, `UserRegisteredKafkaProducer`, `PaymentRetryProducer`) con `@MockitoBean` global en `BaseIntegrationTest`
  - Aísla servicios externos adicionales (`SecurityHelper`, `OutboxProcessor`, `ProxyManager`, `StockNotificationService`) con `@MockitoBean` por test
- Verifican el comportamiento real de JPA, transacciones Spring, eventos de aplicación (`@RecordApplicationEvents`) y el flujo completo service → repository → base de datos
- Pruebas para: Auth (registro, login, refresh, logout, cambio de contraseña), Carrito (CRUD completo con stock y bloqueo pesimista), Órdenes (checkout transaccional), Productos (CRUD con inventario), Inventario (movimientos y eventos de dominio)

### Pruebas de Infraestructura (Testcontainers + Kafka real)
- `EmailKafkaConsumerIntegrationTest` levanta un **Kafka 7.4 real** en contenedor Docker via `@ServiceConnection`
- Verifica el flujo completo productor → tópico → consumidor con idempotencia
- Configuración aislada con `@SpringBootTest(classes = {...})` y auto-configuración limitada
- Usa `await()` para aserciones asíncronas sin sleeps arbitrarios

### Ejecución de Tests

```bash
# Todas las pruebas (requiere Docker en ejecución)
./mvnw clean test

# Prueba de integración específica
./mvnw test -Dtest="AuthServiceImplIntegrationTest"

# Pruebas unitarias únicamente (no requieren Docker)
./mvnw test -Dtest="com.jonathan.ecommerce.service.impl.*"
```

---

## 🚦 Primeros Pasos

### Prerrequisitos

- **Java 21** (Temurin recomendado)
- **Docker Desktop** (o Colima / Rancher Desktop) — **requerido** tanto para el entorno de desarrollo (PostgreSQL, Redis, Kafka via `docker-compose up`) como para la suite de pruebas (Testcontainers levanta contenedores PostgreSQL y Kafka automáticamente)
- **Maven** (o usar el wrapper `mvnw` incluido)

### 1. Clonar y Configurar

```bash
git clone https://github.com/tu-usuario/ecommerce-backend.git
cd ecommerce-backend

# Copiar variables de entorno (editar según sea necesario)
cp .env.example .env
```

### 2. Levantar Infraestructura

```bash
docker-compose up -d
```

Esto inicia:
- **PostgreSQL 16** en `localhost:5444`
- **Redis 7** en `localhost:6379`
- **ZooKeeper + Kafka 7.4** en `localhost:9092`

### 3. Variables de Entorno (`.env`)

```env
DB_HOST=localhost
DB_PORT=5444
DB_NAME=ecommerce
DB_USERNAME=postgres
DB_PASSWORD=tu_password

JWT_ACCESS_TOKEN=clave-secreta-jwt-min-32-caracteres

MAIL_HOST=sandbox.smtp.mailtrap.io
MAIL_PORT=2525
MAIL_USERNAME=tu_mailtrap_user
MAIL_PASSWORD=tu_mailtrap_pass

STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

FRONTEND_URL=http://localhost:5173
```

### 4. Compilar y Ejecutar

```bash
# Compilar (sin pruebas para arranque rápido)
./mvnw package -DskipTests

# Ejecutar
java -jar target/ecommerce-backend-0.0.1-SNAPSHOT.jar

# O directamente con Maven
./mvnw spring-boot:run
```

### 5. Acceder

| Recurso | URL |
|---|---|
| API Base | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Documentación OpenAPI | `http://localhost:8080/v3/api-docs` |

### 6. Ejecutar Pruebas

> **Importante:** Las pruebas de integración requieren Docker en ejecución. Testcontainers gestiona automáticamente los contenedores PostgreSQL y Kafka, sin necesidad de `docker-compose up` previo.

```bash
# Todas las pruebas (252 tests — unitarias + integración)
./mvnw clean test

# Solo pruebas unitarias (no requieren Docker)
./mvnw test -Dtest="com.jonathan.ecommerce.service.impl.*"

# Prueba de integración específica (requiere Docker)
./mvnw test -Dtest="AuthServiceImplIntegrationTest"

# Prueba con Kafka real en contenedor (requiere Docker)
./mvnw test -Dtest="EmailKafkaConsumerIntegrationTest"
```

---

## 📁 Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/jonathan/ecommerce/
│   │   ├── config/              # Seguridad, JWT, Redis, Stripe, Async
│   │   ├── controller/          # REST Controllers (10 endpoints)
│   │   ├── dto/
│   │   │   ├── request/         # DTOs de entrada (record)
│   │   │   ├── response/        # DTOs de salida (record)
│   │   │   ├── event/           # Eventos para Kafka
│   │   │   ├── enums/           # Enums compartidos (OutboxStatus, etc.)
│   │   │   └── mapper/          # MapStruct mappers
│   │   ├── entity/              # JPA Entities (18 entidades)
│   │   │   └── enums/           # Enums de dominio
│   │   ├── exception/           # Manejo global de excepciones
│   │   ├── kafka/
│   │   │   ├── config/          # Topics y configuración Kafka
│   │   │   ├── consumer/        # Consumidores de eventos
│   │   │   ├── processor/       # OutboxProcessor
│   │   │   ├── producer/        # Productores Kafka
│   │   │   └── scheduler/       # OutboxScheduler, EmailRetryScheduler
│   │   ├── ratelimiting/
│   │   │   ├── annotation/      # @RateLimited
│   │   │   ├── aspect/          # RateLimitingAspect (AOP)
│   │   │   ├── config/          # Propiedades de rate limiting
│   │   │   ├── exception/       # RateLimitExceededException
│   │   │   └── filter/          # RateLimitingFilter
│   │   ├── repository/          # Spring Data JPA Repositories
│   │   ├── service/
│   │   │   ├── helper/          # SecurityHelper (contexto de usuario)
│   │   │   └── impl/            # Implementaciones de servicios
│   │   ├── stock/event/         # Eventos de dominio (stock)
│   │   └── util/                # RequestContextUtil
│   └── resources/
│       ├── db/migration/        # Flyway migrations (V1-V10)
│       └── templates/           # Thymeleaf email templates
└── test/
    └── java/com/jonathan/ecommerce/
        ├── config/              # Tests de JwtAuthenticationFilter
        ├── controller/          # @WebMvcTest controllers
        ├── exception/           # GlobalExceptionHandler tests
        ├── kafka/               # Kafka consumer/config tests
        ├── ratelimiting/        # RateLimitingAspect tests
        └── service/             # Unit + Integration tests
```

---

## 📬 API Endpoints

| Método | Ruta | Autenticación | Límite |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Público | 3 req/5min |
| `POST` | `/api/v1/auth/login` | Público | 5 req/min |
| `POST` | `/api/v1/auth/refresh` | Cookie | 100 req/min |
| `POST` | `/api/v1/auth/logout` | Autenticado | — |
| `POST` | `/api/v1/auth/logout-all` | Autenticado | — |
| `PATCH` | `/api/v1/auth/change-password` | Autenticado | — |
| `GET` | `/api/v1/auth/me` | Autenticado | — |
| `GET` | `/api/v1/auth/sessions` | Autenticado | — |
| `GET` | `/api/v1/products/**` | Público | 500 req/min |
| `POST` | `/api/v1/cart/item` | USER/ADMIN | 200 req/min |
| `DELETE` | `/api/v1/cart/item/{id}` | USER/ADMIN | 200 req/min |
| `PUT` | `/api/v1/cart/item/{id}` | USER/ADMIN | 200 req/min |
| `GET` | `/api/v1/cart` | USER/ADMIN | 200 req/min |
| `POST` | `/api/v1/order/checkout` | USER/ADMIN | 20 req/min |
| `GET` | `/api/v1/orders` | Autenticado | — |
| `GET` | `/api/v1/orders/{id}` | Autenticado | — |
| `POST` | `/api/v1/webhooks/stripe` | Público | — |

---

## 🔐 Seguridad

- **Cifrado**: BCrypt para contraseñas
- **JWT**: Access Token (1h) + Refresh Token (24h) rotados en cada uso, firmados con HMAC-SHA256
- **Cookies HttpOnly**: Tokens almacenados en cookies seguras con `HttpOnly`, `Secure` y `SameSite=Strict`
- **CORS**: Configurado para origins de frontend (`localhost:5173`, `localhost:3000`)
- **Bloqueo Pesimista**: `PESSIMISTIC_WRITE` en operaciones de inventario para evitar sobreventa
- **Envío de contraseñas**: Time-based tokens para reseteo, con expiración y uso único
- **Rate Limiting**: Protección asimétrica por endpoint con tres capas (endpoint, usuario, IP)

---

<div align="center">

**© 2026 — Proyecto de Portafolio**  
*Construido con buenas prácticas de desarrollo backend moderno*

</div>
