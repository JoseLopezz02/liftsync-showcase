# LiftSync — Fitness Coaching Platform

> A full-stack SaaS platform for fitness coaches to manage clients, training routines, nutrition plans and progress tracking. Designed, built and operated end-to-end by a single developer, running in production on a self-hosted Linux VPS with a complete CI/CD pipeline.

🌐 **Live at [www.liftsync.es](https://www.liftsync.es)** · Private beta · English & Spanish

---

## What is LiftSync?

LiftSync bridges the gap between fitness coaches and their athletes by putting every tool in one environment. Coaches handle client onboarding, build and assign personalized training blocks and nutrition plans, automate recurring check-in questionnaires, and track athlete progress over time. Athletes log every workout, follow their scheduled diet, and answer automated daily/weekly/monthly questionnaires so the coach's adjustments are based on real, current data.

This repository is a **public showcase** of the system architecture and the engineering behind it. The code here is the *real infrastructure code* running in production — security, caching, messaging, concurrency control, scheduling and tooling. The business layer (services, repositories, domain models) is private to protect the product's core logic.

---

## Project Scale at a Glance

This is not a weekend project. LiftSync has been in continuous development for well over a year and runs as a real production system with real users.

| Metric | Value |
|---|---|
| Active development | **13+ months** of continuous commits (and counting) |
| Commits | **750+** |
| Backend codebase | **310+ Java classes**, ~22,000 lines |
| REST API surface | **130+ endpoints** across 14 controllers |
| Data layer | 27 Spring Data repositories · ~30 JPA entities |
| Schema evolution | **24 versioned Flyway migrations** |
| Domain modules | Auth · Users · Coaching relationships · Training blocks · Nutrition · Questionnaires · Real-time chat · Athlete metrics · Ratings · Admin |
| Infrastructure | MySQL · Redis · RabbitMQ · AWS S3 · WebSocket · SMTP · Docker · GitHub Actions |
| Languages | Full-stack i18n — English & Spanish, backend and frontend |

---

## Architecture Overview

```
                     ┌───────────────────────────────────┐
                     │            CLIENT LAYER           │
                     │  React Web App (EN/ES)  ·  Mobile │
                     └─────────────────┬─────────────────┘
                                       │  HTTPS (REST) · WSS (chat)
                     ┌─────────────────▼─────────────────────────────┐
                     │          BACKEND — Java 21 · Spring Boot 3    │
                     │                                               │
                     │  ┌─────────────────────────────────────────┐  │
                     │  │            SECURITY PIPELINE            │  │
                     │  │  CORS → stateless JWT filter → identity │  │
                     │  │  interceptor → declarative ownership    │  │
                     │  │  rules → relationship guards            │  │
                     │  └─────────────────────────────────────────┘  │
                     │                                               │
                     │   REST Controllers      WebSocket Chat        │
                     │   Business Services (private)                 │
                     │   Scheduled Automations (cron, tz-aware)      │
                     │   Global Exception Handling (i18n messages)   │
                     │                                               │
                     └──┬───────┬────────┬─────────┬──────────┬──────┘
                        │       │        │         │          │
                  ┌─────▼──┐ ┌──▼───┐ ┌──▼─────┐ ┌─▼──────┐ ┌─▼─────────┐
                  │ MySQL  │ │Redis │ │RabbitMQ│ │ AWS S3 │ │  OAuth2   │
                  │ Flyway │ │cache │ │ retry +│ │ pre-   │ │ Google /  │
                  │ migra- │ │layer │ │  DLQ   │ │ signed │ │ Microsoft │
                  │ tions  │ └──────┘ └───┬────┘ │  URLs  │ └───────────┘
                  └────────┘             │      └────────┘
                                    ┌────▼─────┐
                                    │   SMTP   │
                                    │ Thymeleaf│
                                    │ templates│
                                    └──────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA |
| Auth | OAuth2 (Google + Microsoft), JWT (stateless), refresh tokens |
| Database | MySQL + Flyway versioned migrations |
| Caching | Redis (type-safe, transaction-aware, versioned keys) |
| Messaging | RabbitMQ (retry topology + dead-letter queue) |
| Cloud Storage | AWS S3 with server-side pre-signed URLs |
| Real-time | Raw WebSocket (coach–athlete–admin chat) |
| Email | SMTP + Thymeleaf HTML templates, localized, async via RabbitMQ |
| Frontend | React (JavaScript), Lingui i18n |
| Mobile | React Native (in progress) |
| Quality | SonarQube static analysis wired into the Maven build |
| Containerisation | Docker, Docker Compose (dev & prod) |
| CI/CD | GitHub Actions → self-hosted Linux VPS |

---

# Backend Engineering Deep Dives

Everything below is real production code, included in this repository.

## 1. Layered Security Architecture

Authorization is not a single check — it is a pipeline where each layer has one responsibility. A request only reaches a controller after passing every applicable layer:

```
 Request
   │
   ▼
 ① SecurityConfig            CORS, stateless sessions, deny-by-default route policy
 ② JwtAuthenticationFilter   verifies the JWT, loads the user, populates the SecurityContext
 ③ TokenInterceptor          resolves a minimal identity projection (id, publicId, role)
 ④ UserOwnershipInterceptor  declarative, ordered rules: role gating + resource ownership
 ⑤ @RequireCoachingRelationship  annotation-driven guard: is this coach *actually* this
   (CoachingRelationshipInterceptor)  athlete's coach, with an accepted relationship?
   │
   ▼
 Controller — already authenticated AND authorized
```

**Deny-by-default filter chain** — every route must be explicitly public, everything else requires authentication, and anything outside the API surface is rejected outright:

```java
// SecurityConfig.java
return http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session
                -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
                .requestMatchers(SecurityPaths.publicApiPaths()).permitAll()
                .requestMatchers(SecurityPaths.WS_PATTERN).permitAll()
                .requestMatchers(SecurityPaths.API_PATTERN).authenticated()
                .anyRequest().denyAll()
        )
        .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
```

**Declarative ownership rules** — instead of scattering `if` checks across controllers, resource-level authorization is a single ordered rule table. Path variables are constrained to the UUID shape so literal segments can never match an id capture, and any id in a path must equal the caller's own public id:

```java
// UserOwnershipInterceptor.java — rules evaluated top-down, most specific first
private final List<Rule> rules = List.of(

        new Rule("/api/coaches/pending", null,
                EnumSet.of(Role.COACH_PENDING), false),

        // Athlete personal info: athlete owns it; coach may read it
        // (relationship verified by @RequireCoachingRelationship on the handler)
        new Rule("/api/athletes/{athleteId:" + UUID_RX + "}/personalInfo", "athleteId",
                EnumSet.of(Role.ATHLETE), true),
        new Rule("/api/athletes/{athleteId:" + UUID_RX + "}/personalInfo", "athleteId",
                EnumSet.of(Role.COACH), false),

        // Any other resource carrying a publicId must be the caller's own (defense in depth)
        new Rule("/api/athletes/{athleteId:" + UUID_RX + "}/**", "athleteId",
                EnumSet.of(Role.ATHLETE), true),
        new Rule("/api/admin/{adminId:" + UUID_RX + "}/**", "adminId",
                EnumSet.of(Role.ADMIN), true),

        // Role-only fallbacks — caller identity comes from the JWT, not the URL
        new Rule("/api/coaches/**", null, EnumSet.of(Role.COACH), false),
        new Rule("/api/athletes/**", null, EnumSet.of(Role.ATHLETE, Role.COACH), false),
        new Rule("/api/admin/**", null, EnumSet.of(Role.ADMIN), false));
```

**Annotation-driven relationship guard** — coach-scoped endpoints declare their contract instead of implementing it. The interceptor resolves the athlete from the path, verifies an *accepted* coaching relationship exists with the caller, and rejects otherwise:

```java
@GetMapping("/athletes/{athleteId}/metrics")
@RequireCoachingRelationship            // athleteIdParam defaults to "athleteId"
public ResponseEntity<...> getAthleteMetrics(@PathVariable String athleteId) { ... }
```

**Design decisions worth noting:**

- **UUID public identifiers everywhere.** Sequential database ids never leave the backend, which kills enumeration/IDOR attacks at the design level rather than patching them per-endpoint.
- **Identity travels as a minimal projection** (`AuthUserProjection` — publicId + role), so per-request auth never hydrates full entities.
- **Every rejection is localized** (EN/ES) through the same `MessageResolver` used by the rest of the app.

📂 `backend/src/main/java/com/liftsync/config/security/` · `config/interceptor/` · `validation/`

---

## 2. Asynchronous Messaging — RabbitMQ with Retry + DLQ

Transactional email (verification codes, password recovery, coach invites, onboarding) never blocks an HTTP request and never silently disappears. Publishing is decoupled from delivery with a full retry topology:

```
                    publish (after DB commit)
                            │
                            ▼
              exchange ──► queue ──► EmailListener
                            ▲          │
                            │          ├─ success ──► SMTP (Thymeleaf, localized)
        x-dead-letter back  │          │
        to main exchange    │          ├─ invalid message ────────────► DLQ
                            │          │
                    retry.queue        └─ failure:
                    (TTL = backoff)         retry count ≤ max ──► retry.exchange
                            ▲               retry count > max ──► DLQ
                            └───────────────────┘
```

- **The topology is declared in code, generically.** `MessagingTopology.declarables()` builds the exchange/queue/binding set for *any* message domain from a properties interface — the email topology is just one instantiation. Adding a new async domain (e.g. push notifications) is a properties record + one `@Bean`.

```java
// MessagingTopology.java — one generic topology for any messaging domain
var retryQueue = QueueBuilder.durable(properties.retryQueue())
        .withArguments(Map.of(
                "x-message-ttl", properties.retryDelayMs(),
                "x-dead-letter-exchange", properties.exchange(),
                "x-dead-letter-routing-key", properties.routingKey()
        ))
        .build();
```

- **Retries are broker-driven, not thread-driven.** A failed message is published to a TTL'd retry queue whose dead-letter target is the main exchange — the broker itself re-delivers after the backoff, with the retry count carried in a header. No sleeping consumer threads.
- **Transaction-aware publishing.** `EmailPublisher.publish()` registers the send as an after-commit synchronization: if the surrounding DB transaction rolls back, the email is never sent. No more "verification email for a user that doesn't exist".
- **Poison messages are quarantined, not requeued.** Validation failures skip the retry loop and go straight to the DLQ with the failure reason attached as a header, so they can be inspected without blocking the queue.
- **Type-safe email contracts.** Each `EmailType` declares its required template variables; messages are validated against that contract before dispatch. The dispatcher renders the Thymeleaf template in the *recipient's* locale (stored per message), restoring the previous locale afterwards.

📂 `backend/src/main/java/com/liftsync/messaging/` · `config/rabbitmq/`

---

## 3. Distributed Caching Layer (Redis)

Production-grade caching infrastructure with **type-safe deserialisation**, **transaction-aware eviction** and **versioned cache invalidation** — solving the real production problems most caching layers ignore: class-name drift on serialized values, race conditions between eviction and commit, and fleet-wide invalidation after incompatible deploys.

**Core design decisions:**

1. **Type-safe deserialisation** — every cache name is bound to a concrete Jackson `JavaType` at startup via `CacheSpec`, eliminating class-name-based deserialisation and runtime `ClassCastException` surprises:

```java
// Caches.java — central registry (16 caches in production)
register(CacheSpec.of(USER_DTO_BY_PUBLIC_ID, UserDTO.class));
register(CacheSpec.of(ATHLETE_METRICS, AthleteMetricsResp.class));
register(CacheSpec.ofList(BLOCK_LIBRARY, BlockTemplatePreviewDTO.class));
register(CacheSpec.ofMap(HIGH_INTENSITY_TECHNIQUES, String.class, String.class));
```

2. **Startup-time safety check** — reflection asserts that every declared cache-name constant has a registered spec. A misconfigured cache fails the boot, not a 3 a.m. request:

```java
// Caches.java
if (!missing.isEmpty()) {
    throw new IllegalStateException(
            "Caches: the following name constants have no registered CacheSpec — "
                    + "add a register(...) call in the static initializer: " + missing);
}
```

3. **Transaction-aware eviction** — evictions register as transaction synchronizations and fire only *after commit*, preventing concurrent readers from repopulating the cache with pre-commit data:

```java
// CacheEvictor.java
public void evictAthleteMetricsByAthleteAndCoachAfterCommit(Long athleteId, Long coachId) {
    runAfterCommit(() -> evictAthleteMetricsByAthleteAndCoach(athleteId, coachId));
}
```

4. **Versioned key prefixes** — every key is `liftsync:<version>:<cacheName>::<key>`. Bumping one property invalidates the entire cache fleet instantly after a deploy with incompatible serialisation — no manual `FLUSHALL`, no stale-shape reads.

5. **Per-cache TTLs, externally configured** — different caches have different staleness tolerances; TTLs live in properties with a sane default, not in code.

6. **Kill switch** — the whole layer sits behind `app.cache.enabled`. With caching off, the app runs uncached with zero code changes (the evictor resolves its `CacheManager` through `ObjectProvider`).

📂 `backend/src/main/java/com/liftsync/cache/` · `config/cache/`

---

## 4. Concurrency Control — Optimistic Locking Done Properly

Coaches and athletes edit the same aggregates (a coach adjusts next week's plan while the athlete logs tonight's sets), and midnight automations touch the same rows as night-owl users. Concurrency is handled at three levels:

**Version-checked API requests.** Mutating requests carry the entity version the client last saw (`VersionedIdRequest`, `VersionedPublicIdRequest`). A helper validates it against the current state and translates mismatches into a localized HTTP `409 Conflict` the frontend understands:

```java
// VersionValidationHelper.java
if (currentVersion == null || requestVersion.longValue() != currentVersion.longValue()) {
    throw new ConflictException(messageResolver.getMessage(conflictMessageKey));
}
```

**Aggregate-root version bumps.** Editing a child (a set, an exercise) doesn't dirty the root, so plain `@Version` wouldn't detect two coaches editing different children of the same block. The lock helper forces a version increment on the root and flushes, making concurrent writers collide deterministically:

```java
// EntityVersionLockHelper.java
public void forceIncrementVersion(Object entity, String conflictMessageKey) {
    try {
        entityManager.lock(entity, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        entityManager.flush();
    } catch (OptimisticLockException e) {
        throw new ConflictException(messageResolver.getMessage(conflictMessageKey));
    }
}
```

**Contention-aware background jobs.** The weekly block-clone automation runs at midnight — exactly when active users may still be logging sets. Instead of failing or blocking, it retries on optimistic-lock collisions with a short backoff, and any surviving `ObjectOptimisticLockingFailureException` anywhere in the app is mapped to a clean localized `409` by the global handler.

Schema support arrived through Flyway: one migration adds `version` columns to the contended tables, another hardens idempotency with unique constraints and adds the composite indexes the authorization queries need.

📂 `backend/src/main/java/com/liftsync/util/` · `dto/request/common/`

---

## 5. Real-time Chat — WebSocket with Handshake-level Auth

Raw WebSocket (no STOMP) with a custom `AuthHandshakeInterceptor` that authenticates *and authorizes* before the connection is ever established — unauthenticated or unauthorized users never open a socket:

1. JWT is present and valid → else `401`
2. The user exists → else `401`
3. The requested room exists — looked up by **UUID public id**, never a sequential id → else `404`
4. The user is a participant of *that specific room* → else `403`

Identity and room are then stored as session attributes, so the message handler never re-parses the token. The handler itself keeps sessions in a `ConcurrentHashMap`-backed set, broadcasts only to the target room, and prunes dead connections as it iterates:

```java
// ChatWebSocketHandler.java
for (Iterator<WebSocketSession> it = sessions.iterator(); it.hasNext(); ) {
    WebSocketSession ws = it.next();
    if (!ws.isOpen()) { it.remove(); continue; }

    Long theirRoom = (Long) ws.getAttributes().get("roomId");
    if (roomId.equals(theirRoom)) {
        try {
            ws.sendMessage(outMsg);
        } catch (IOException | IllegalStateException ex) {
            it.remove();
        }
    }
}
```

📂 `backend/src/main/java/com/liftsync/config/websocket/`

---

## 6. Scheduled Automation

The platform runs itself: recurring questionnaires are generated, training weeks are cloned forward, and stale routines/schedules are deactivated — all on timezone-aware cron schedules:

```java
// SchedulingConfig.java
@Scheduled(cron = "0 0 0 ? * FRI", zone = "Europe/Madrid")
public void scheduleWeeklyBlockClone() { ... }

@Scheduled(cron = "0 0 0 * * ?", zone = "Europe/Madrid")
public void scheduleDailyQuestionnaires() { ... }

@Scheduled(cron = "0 0 3 * * ?", zone = "Europe/Madrid")
public void setInactiveQuestionnaires() { ... }
```

The automation internals are part of the private business layer, but the engineering constraints they solve are worth stating: idempotent generation (an assignment for a given schedule+date is created at most once), catch-up semantics for missed dates without creating past-dated work, self-healing deactivation of orphaned schedules, and the optimistic-lock retry loop described in §4 for jobs that contend with live users.

📂 `backend/src/main/java/com/liftsync/scheduling/`

---

## 7. Error Handling Architecture

One base exception carries its HTTP status, so the global handler maps the entire domain-exception hierarchy with a *single* handler — no per-type boilerplate, no status codes scattered through controllers:

```java
// ApiException.java — the status travels with the exception
public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;
    ...
}

// GlobalExceptionHandler.java — one handler for the whole hierarchy
@ExceptionHandler(ApiException.class)
public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
    return buildError(ex.getStatus(), ex.getMessage());
}
```

Around that core: safety nets for Spring's own `ResponseStatusException`, JPA's `EntityNotFoundException` (guaranteed `404` instead of a `500`), optimistic-lock failures (`409`), client aborts (logged at debug, not error), and a last-resort `500` that logs the stack trace but returns only a localized generic message — internals never leak to clients. Every user-facing message goes through `MessageResolver` and is served in the caller's locale.

📂 `backend/src/main/java/com/liftsync/exception/` · `config/GlobalExceptionHandler.java`

---

## 8. Database Schema Management

- **24 versioned Flyway migrations** — every schema change in the project's history is reproducible from scratch, on any environment, in order.
- **Idempotency hardening as a migration** — unique constraints on natural keys (user email/username/phone, coach–athlete pairs, token email+purpose) plus the composite indexes that back the authorization and relationship queries.
- **Concurrency as a migration** — `version` columns added to contended aggregates to support the locking strategy in §4.
- **Audit via `@MappedSuperclass`** — `CreationAudited` / `FullAudited` base classes give entities creation/modification timestamps declaratively (`@CreationTimestamp`, `@UpdateTimestamp`), not via copy-pasted columns.

📂 `backend/src/main/java/com/liftsync/model/audit/`

---

## 9. Developer Experience & Observability Tooling

Custom tooling built for this project — because finding an N+1 query should not require reading 400 lines of Hibernate logs:

**SQL query tracer with N+1 detection.** A Hibernate `StatementInspector` (registered only in dev) feeds a per-thread trace. Wrap any code path and get a grouped report — every statement, how many times it ran, and the exact application line that triggered it:

```
════════════════════════════════════════════════════════════════════
 [Debug] 27 queries (4 unique) in 63.2 ms · save block
════════════════════════════════════════════════════════════════════
  1.      select ... from block where ...
     ↳ BlockService.saveBlock(BlockService.java:214)
  2. x24  select ... from series where exercise_id=?
     ↳ ExerciseMapper.toDto(ExerciseMapper.java:87)
  ...
════════════════════════════════════════════════════════════════════
 ⚠ 1 statement(s) repeated — 24 of 27 executions. Possible N+1.
════════════════════════════════════════════════════════════════════
```

*(illustrative output — the tool prints exactly this format)*

**Dump-and-die debugging with a production tripwire.** A Laravel-style `Debug.dd(...)` that serializes any object graph (Jackson with reflection fallback) and unwinds out of the controller. The flag is wired from the environment at startup: outside dev it becomes a no-op that *warns loudly* — a forgotten debug call can never leak data in production.

**One-command dev environment.** `docker-compose.dev.yml` + Makefile bring up the full stack — MySQL, Redis, RabbitMQ (management UI), MailHog for email capture, nginx, backend with hot-reload (volume-mounted source + DevTools), frontend with Vite watch. `make recompile` triggers an in-container incremental rebuild.

📂 `backend/src/main/java/com/liftsync/util/query/` · `util/debug/` · `config/DebugConfigurer.java`

---

## 10. Full-Stack Internationalisation

Every user-facing string — API errors, validation messages, transactional emails, the entire frontend — exists in English and Spanish.

**Backend:** `AcceptHeaderLocaleResolver` resolves the locale per request; a thin `MessageResolver` wraps `MessageSource` + `LocaleContextHolder` so every layer (exceptions, interceptors, email rendering) localizes through one seam. Async email is the interesting case: the recipient's locale is *stored in the message* and activated by the dispatcher at render time — the email arrives in the user's language regardless of which locale the consumer thread had.

**Frontend:** Lingui with `.po` catalogs compiled to `.mjs` at build time in CI. The locale is persisted client-side and translations load before first render — no flash of untranslated content.

📂 `backend/src/main/java/com/liftsync/config/i18n/` · `frontend/src/translations/`

---

## CI/CD Pipeline

Backend and frontend deploy independently via GitHub Actions (`workflow_dispatch`), targeting a self-hosted Linux VPS.

```
Backend:  checkout → JDK 21 (Temurin, Maven cache) → mvn package
          → SCP JAR + compose file to VPS
          → inject .env from GitHub Secrets (18 secrets)
          → docker compose down && up -d --build

Frontend: checkout → Node → npm ci → extract + compile i18n (.po → .mjs)
          → verify catalogs → build → clean old dist on VPS → SCP dist/
```

**Secrets management:** DB credentials, JWT secret, both OAuth2 providers, AWS keys, SMTP credentials, RabbitMQ credentials, timezone — 18 values injected at deploy time from GitHub Secrets. Nothing sensitive exists in the repository, the image, or the VPS beyond the generated `.env`.

📂 `.github/workflows/`

---

## Production Infrastructure

| Component | Details |
|---|---|
| Hosting | Self-hosted Linux VPS |
| Domain | liftsync.es (custom DNS, HTTPS) |
| Backend | Spring Boot JAR in Docker |
| Frontend | Static React build behind nginx |
| Database | MySQL in Docker, Flyway-managed schema |
| Cache | Redis in Docker |
| Messaging | RabbitMQ in Docker |
| Storage | AWS S3 (pre-signed, time-limited URLs — per-asset-type expirations) |
| Orchestration | Docker Compose |
| Deployments | GitHub Actions, manual trigger |
| Secrets | GitHub Secrets → injected at deploy time |

---

## Repository Structure

```
liftsync-showcase/
│
├── .github/workflows/               # Real CI/CD pipelines (secrets referenced, never stored)
│
├── backend/src/main/java/com/liftsync/
│   ├── LiftSyncBackendApplication.java   # Entry point + interceptor pipeline wiring
│   │
│   ├── config/
│   │   ├── security/                # Filter chain, JWT filter, route policy, entry point
│   │   ├── interceptor/             # Token identity · ownership rules · relationship guard
│   │   ├── cache/                   # Redis manager, type-safe specs, registry, TTLs
│   │   ├── rabbitmq/                # Broker config + typed messaging properties
│   │   ├── websocket/               # Handshake auth + chat handler + registration
│   │   ├── amazon/                  # S3 client/presigner + per-asset URL expirations
│   │   ├── i18n/                    # Locale resolution + MessageResolver seam
│   │   ├── GlobalExceptionHandler.java
│   │   ├── TransactionManager.java  # after-commit synchronization helper
│   │   └── DebugConfigurer.java     # dev-only tooling wiring (with prod tripwire)
│   │
│   ├── messaging/
│   │   ├── common/MessagingTopology.java  # Generic exchange/retry/DLQ topology builder
│   │   └── email/                   # Publisher, listener, dispatcher, typed contracts
│   │
│   ├── cache/                       # Transaction-aware evictor + key utils
│   ├── exception/                   # ApiException hierarchy (status travels with the type)
│   ├── model/audit/                 # Auditing @MappedSuperclass bases
│   ├── scheduling/SchedulingConfig.java   # Timezone-aware cron automations
│   ├── util/                        # Version validation + entity lock helpers
│   │   ├── query/                   # SQL tracer with N+1 detection
│   │   └── debug/                   # Dump-and-die tooling
│   ├── validation/                  # @RequireCoachingRelationship
│   └── dto/…/common/                # Versioned request contracts, error envelopes
│
├── frontend/src/                    # i18n setup (Lingui) + app bootstrap
│
└── README.md
```

> The business layer — 19 services, 27 repositories, ~30 entities, 130+ endpoints — is private. What you see here is the engineering that carries it.

---

## Live Application

The full platform is live at **[www.liftsync.es](https://www.liftsync.es)**

- Free tier available — sign up as a Coach or Athlete
- Google and Microsoft single sign-on
- Available in English and Spanish
- Pro and Team tiers coming soon

---

## About the Developer

Built and maintained by **José** — backend software engineer based in Mallorca, Spain.

Currently working on the IKEA regional e-commerce platform (Sarton) across 5+ territories, and building LiftSync as a domain-expert side project.

📧 [jjoselopezbt@gmail.com](mailto:jjoselopezbt@gmail.com) · 💼 [linkedin.com/in/jose-lopez-backend](https://linkedin.com/in/jose-lopez-backend) · 🌐 [liftsync.es](https://www.liftsync.es)

---

*This repository is a public showcase. Full source code is private.*
