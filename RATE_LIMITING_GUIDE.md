# Rate Limiting Architecture Guide

## 1. Two-Layer Architecture

The system combines **two independent enforcement layers** that can overlap:

```
HTTP Request
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│  SecurityFilterChain (Spring Security)                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  RateLimitingFilter ← BEFORE JwtAuthenticationFilter     │   │
│  │  (enforces limits from application.yml, path-based)      │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  JwtAuthenticationFilter                                  │   │
│  │  (authenticates user, populates SecurityContext)          │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│  Spring MVC Controller                                         │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  @RateLimited("endpoint-key") method                     │   │
│  │  → RateLimitingAspect (AOP, around advice)               │   │
│  │  (enforces limits from application.yml, key-based)       │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Filter (`RateLimitingFilter`)

- Extends `OncePerRequestFilter`, placed **before** `JwtAuthenticationFilter` in the chain (`SecurityConfig.java:78`).
- Runs for **every request** except `/health` and `/actuator/health`.
- Resolves limits via `RateLimitingProperties.getLimitForEndpoint(requestPath)`, which matches the URI against patterns in `application.yml` (exact match first, then wildcard `*` suffix).
- Checks **3 layers** (`endpoint` → `user` → `ip`). If any fails → HTTP 429 JSON response.
- User-layer relies on `SecurityContextHolder` populated by the **next** filter in the chain. Before JWT authentication, `getUserId()` returns `null`, so unauthenticated requests use `"anonymous"` as the user key.

### Aspect (`RateLimitingAspect`)

- `@Around` advice triggered by the `@RateLimited("key")` annotation on any Spring-managed method.
- Looks up the key in `RateLimitingProperties.getLimitForEndpoint(key)` — the same shared config.
- Checks the same 3 layers (`endpoint` → `user` → `ip`) within the Aspect context, where the user is already authenticated.
- **Overlap with Filter**: An endpoint protected by the Filter AND annotated with `@RateLimited` gets double-enforced. The Filter catches unauthenticated requests early; the Aspect provides method-level granularity.

### Configuration (`application.yml`)

```yaml
app:
  rate-limiting:
    enabled: true
    defaults:
      endpoint-capacity: 100
      endpoint-refill-minutes: 1
      user-capacity: 500
      user-refill-minutes: 1
      ip-capacity: 1000
      ip-refill-minutes: 5
    endpoints:
      "/api/v1/auth/login":
        capacity: 5
        refill-minutes: 1
        user-capacity: 10
        ip-capacity: 50
      "/api/v1/auth/register":
        capacity: 3
        refill-minutes: 5
        ip-capacity: 20
      "/api/v1/order/checkout":
        capacity: 20
        user-capacity: 10
        ip-capacity: 100
        fail-open: false
```

- **Exact paths** (`/api/v1/auth/login`) and **wildcard paths** (`/api/v1/cart/**`) are both supported.
- `user-capacity: 0` disables the user-level check for that endpoint.
- `fail-open: false` (default: `true`) changes behavior from "allow on error" to "reject on error".

---

## 2. Distributed Token Bucket (Bucket4j + Redis)

### Algorithm

Greedy Token Bucket with **interval refill**:

```
Bandwidth.classic(capacity, Refill.intervally(tokens, Duration.ofMinutes(minutes)))
```

The bucket starts full (`capacity` tokens). Every `minutes` minutes, exactly `tokens` tokens are restored (not gradually — all at once at the interval boundary). Each request consumes 1 token. When the bucket is empty, `tryConsume(1)` returns `false` → 429.

### Distributed State

| Component | Role |
|---|---|
| `RedisConfig.redisClient()` | Creates a Lettuce `RedisClient` from `spring.data.redis.host:port`. |
| `RedisConfig.redisConnection()` | Opens a native `StatefulRedisConnection<byte[], byte[]>` using `ByteArrayCodec`. |
| `BucketConfig.proxyManager()` | Builds a `LettuceBasedProxyManager` with 1-hour TTL on bucket keys via `ExpirationAfterWriteStrategy`. |

### Key Structure

| Scope | Redis Key Pattern |
|---|---|
| Endpoint | `endpoint:<path-or-key>` |
| User | `user:<userId>:<path-or-key>` |
| IP | `ip:<clientIp>` |

All keys use `byte[]` encoding with `UTF-8`. The 1-hour TTL on each bucket key prevents stale keys from accumulating in Redis.

### Example: Login endpoint behavior

Assuming `capacity=5, refill-minutes=1, refill-tokens=5`:

```
Request#1  → token consumed → 4 remaining
Request#2  → token consumed → 3 remaining
...
Request#6  → bucket empty → 429 Too Many Requests
60 seconds later → 5 tokens refilled → bucket full again
```

---

## 3. Fail-Open Strategy

Both enforcement points wrap the entire rate-limit check in `try/catch`. On any exception (Redis down, network timeout, serialization failure):

```java
catch (Exception ex) {
    if (config.getFailOpen()) {
        log.warn("Rate limit check failed (fail-open). Allowing request.");
        return joinPoint.proceed();  // or filterChain.doFilter()
    } else {
        throw new RateLimitExceededException("Service unavailable");
    }
}
```

| Scenario | `failOpen` | Behavior |
|---|---|---|
| Redis goes down | `true` (default) | Request proceeds normally. Logged as warning. |
| Redis goes down | `false` | Request rejected with HTTP 429. Logged as error. |
| Redis recovers | either | Next request creates a fresh bucket in Redis (buckets are auto-vivified by Bucket4j). |

**Default**: All endpoints except `/api/v1/order/checkout` use `failOpen: true`. The checkout endpoint is set to `failOpen: false` to avoid processing orders without rate-limit protection (e.g., during a Redis outage).

### Impact on Business Logic

- **Fail-open mode** means a Redis outage does not block any traffic. The only observable effect is that rate-limit counters reset to zero — all clients effectively get a full bucket.
- **Fail-closed mode** (`/api/v1/order/checkout`) means during a Redis outage, every request to that endpoint gets HTTP 429. Trading availability for rate-limit integrity.

---

## 4. Applying `@RateLimited` to a New Controller

### Step 1: Add configuration in `application.yml`

```yaml
app:
  rate-limiting:
    endpoints:
      "/api/v1/reviews":
        capacity: 30
        refill-minutes: 1
        user-capacity: 10
        user-refill-minutes: 1
        ip-capacity: 100
        ip-refill-minutes: 5
```

### Step 2: Annotate the controller method

```java
@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    @RateLimited("/api/v1/reviews")
    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody ReviewDTO dto) {
        // ...
    }
}
```

The `@RateLimited` value is a key that maps to `endpoints["/api/v1/reviews"]` in `RateLimitingProperties`. If no matching key is found, the `defaults` are applied.

### Step 3: Optional — protect only public endpoints with the Filter

If the endpoint is **public** (no authentication required), the `RateLimitingFilter` is the enforcement layer (since the Aspect runs after authentication). Just add the path to `application.yml` — no annotation needed.

```yaml
"/api/v1/public/search":
  capacity: 50
  refill-minutes: 1
  ip-capacity: 30
  ip-refill-minutes: 1
```

### Step 4: Per-method granularity

For endpoints with different sensitivities on the same controller:

```java
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @RateLimited("/api/v1/payments/checkout")
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody PaymentDTO dto) { ... }

    @RateLimited("/api/v1/payments/history")
    @GetMapping("/history")
    public ResponseEntity<?> getHistory() { ... }
}
```

Each method uses a different key, allowing per-endpoint limits in `application.yml`.

---

## 5. Security & Performance Considerations

- **Redis latency**: Every rate-limited request adds 1–3 Redis round-trips (one per active layer). On a local Redis instance this is <1ms. On a remote Redis, latency adds up — consider enabling only the layers you need per endpoint (disable user/ip with `capacity: 0`).
- **Filter ordering**: `RateLimitingFilter` runs **before** `JwtAuthenticationFilter` (`SecurityConfig.java:78`). This means user-level checks fall back to `"anonymous"` until the JWT is resolved. If user-level enforcement is critical for public endpoints, use the `@RateLimited` aspect instead (which runs inside the authenticated context).
- **Key TTL**: Bucket keys expire 1 hour after last write (`BucketConfig.java:26`). Idle endpoints self-clean from Redis.
- **Wildcard matching**: `RateLimitingProperties.pathMatches()` uses simple prefix matching (`startsWith`). If you have routes like `/api/v1/orders` and `/api/v1/orders-finalize`, the `*` wildcard on `/api/v1/orders/*` will match both — order your exact paths first in `endpoints` to get the exact match.
