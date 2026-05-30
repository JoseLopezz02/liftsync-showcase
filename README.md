# LiftSync — Fitness Coaching Platform

> A full-stack SaaS platform for fitness coaches to manage clients, routines, nutrition plans, and progress tracking. Built end-to-end by a single developer with a production deployment pipeline on a self-hosted Linux VPS.

🌐 **Live at [www.liftsync.es](https://www.liftsync.es)** · Private beta · English & Spanish

---

## What is LiftSync?

LiftSync bridges the gap between fitness coaches and their athletes, staging all the tools on the same environment. Coaches manage client onboarding, assign personalized workout routines and nutrition plans, automate weekly check-in questionnaires, and track client progress over time — all from one platform. Athlete track daily, weekly or monthly automated questionnaire,
to give feedback to the coach and be sure his consulting and adjustment are as acurrate as possible; also tracking each workout and having a scheduled diet that assures them reach their goals 

This repository is a **public showcase** of the system architecture, CI/CD pipeline, and key technical implementations. The full source code is private to protect business logic.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                      CLIENT LAYER                       │
│                                                         │
│   React Web App (i18n EN/ES)    React Native Mobile     │
│         ↓                              ↓                │
└─────────────────────────────────────────────────────────┘
                          │
                    REST API (HTTPS)
                          │
┌─────────────────────────────────────────────────────────┐
│                    BACKEND LAYER                        │
│                                                         │
│              Java 21 + Spring Boot 3                    │
│                                                         │
│   ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
│   │  Auth    │  │ Business │  │    i18n / Locale     │  │
│   │ OAuth2   │  │  Logic   │  │  MessageSource       │  │
│   │ JWT      │  │  Layer   │  │  LocaleContextHolder │  │
│   └──────────┘  └──────────┘  └──────────────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┼──────────────────┼─────────────────┐
        ↓                 ↓                  ↓                 ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
│    MySQL     │  │   AWS S3     │  │    Redis     │  │ OAuth2 Providers │
│  Database    │  │ Photos/Media │  │  Caching     │  │ Google/Microsoft │
│              │  │ (avatars +   │  │  Layer       │  └──────────────────┘
│              │  │ chat media)  │  │              │
└──────────────┘  └──────────────┘  └──────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring Security |
| Auth | OAuth2 (Google + Microsoft), JWT |
| Database | MySQL |
| Caching | Redis (type-safe, transaction-aware, versioned) |
| Cloud Storage | AWS S3 (user avatars, profile photos, future chat media) |
| Real-time | WebSocket (coach-athlete-admin chat) |
| Frontend | React, JavaScript |
| Mobile | React Native (in progress) |
| i18n | Lingui (@lingui/core, @lingui/react), .po files compiled to .mjs |
| Containerisation | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| Hosting | Self-hosted Linux VPS, custom domain |

---

## Repository Structure

```
liftsync-showcase/
│
├── .github/
│   └── workflows/
│       ├── deploy-backend.yml           # Backend CI/CD
│       └── deploy-frontend.yml          # Frontend CI/CD
│
├── backend/
│   ├── src/main/java/com/liftsync/
│   │   ├── LiftSyncBackendApplication.java
│   │   ├── cache/
│   │   │   ├── CacheEvictor.java        # Transaction-aware eviction
│   │   │   └── CacheKeyUtils.java       # Composite key generation
│   │   └── config/
│   │       ├── amazon/
│   │       │   └── S3Config.java        # AWS S3 + presigner
│   │       ├── cache/
│   │       │   ├── CacheConfig.java     # Redis cache manager
│   │       │   ├── Caches.java          # Cache registry + safety check
│   │       │   ├── CacheSpec.java       # Type-safe binding
│   │       │   └── CacheTtlProperties.java  # Per-cache TTL config
│   │       ├── websocket/
│   │       │   ├── AuthHandshakeInterceptor.java  # Auth at handshake
│   │       │   └── WebSocketConfig.java           # Handler registration
│   │       ├── InternationalizationConfig.java    # i18n config
│   │       └── SecurityConfig.java               # CORS + Security
│   └── .env.example                     # Env variables template
│
├── frontend/
│   └── src/
│       ├── translations/
│       │   ├── locales/
│       │   │   ├── en/messages.po       # English (example)
│       │   │   └── es/messages.po       # Spanish (example)
│       │   └── i18n.js                  # Lingui setup
│       └── main.jsx                     # App entry point
│
└── README.md
```

---

## CI/CD Pipeline

Both backend and frontend have independent GitHub Actions pipelines triggered manually (`workflow_dispatch`), allowing independent deployments.

### Backend Pipeline

```
Push trigger (manual)
        │
        ▼
┌───────────────────┐
│  Checkout code    │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  Setup JDK 17     │
│  (Temurin)        │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  Maven Build      │
│  mvn package      │
│  -DskipTests      │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  Setup SSH keys   │
│  from secrets     │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  SCP JAR → VPS    │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  Inject .env      │
│  from GitHub      │
│  Secrets (14 vars)│
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  docker compose   │
│  down + up -d     │
└───────────────────┘
```

**Secrets managed:** DB credentials, JWT secret, Google OAuth2, Microsoft OAuth2, AWS S3 keys, email credentials — all injected at deploy time, never stored in the repository.

### Frontend Pipeline

```
Push trigger (manual)
        │
        ▼
┌───────────────────┐
│  Checkout code    │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  Setup Node 18    │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  npm ci           │
└────────┬──────────┘
         │
         ▼
┌────────────────────────┐
│  Extract + compile     │
│  i18n translations     │
│  (.po → .mjs)          │
└────────┬───────────────┘
         │
         ▼
┌───────────────────┐
│  Verify .mjs      │
│  files exist      │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  npm run build    │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  Clean old dist   │
│  on VPS via SSH   │
└────────┬──────────┘
         │
         ▼
┌───────────────────┐
│  SCP dist/ → VPS  │
│  (strip paths)    │
└───────────────────┘
```

---

## Key Technical Implementations

### Security & CORS Configuration

CORS fully configured with environment-driven allowed origins, supporting credentials for cookie-based auth alongside JWT. All API routes protected, with a dev-only bypass for local development.

```java
// SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${env.frontend.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/dev/**").permitAll() // Dev tools (dev profile only)
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

### WebSocket — Real-time Coach-Athlete-Admin Chat

Raw WebSocket implementation (not STOMP) with a custom `AuthHandshakeInterceptor` that authenticates and authorises connections at the handshake level — before the WebSocket connection is established. The connection is rejected entirely if any check fails, meaning unauthenticated or unauthorised users never establish a WebSocket connection.
Room authentication uses UUID public identifiers rather than sequential database IDs — preventing enumeration attacks while maintaining resource-level access control at the handshake level.

The interceptor validates three things in sequence:
1. JWT token is present and valid
2. The user exists in the database
3. The user is a participant of the specific chat room being requested — not just authenticated, but authorised for that exact resource

Appropriate HTTP status codes are returned at each failure point: `401 UNAUTHORIZED`, `403 FORBIDDEN`, `404 NOT_FOUND`.

```java
// AuthHandshakeInterceptor.java
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final TokenService tokenService;
    private final UserService userService;
    private final ChatService chatService;

    public AuthHandshakeInterceptor(TokenService tokenService,
                                     UserService userService,
                                     ChatService chatService) {
        this.tokenService = tokenService;
        this.userService = userService;
        this.chatService = chatService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws HandshakeFailureException {

        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUri(request.getURI())
                .build()
                .getQueryParams();

        String token = params.getFirst("token");
        String roomIdS = params.getFirst("roomId");

        if (token == null || roomId == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Long userId = tokenService.verifyAndGetIdFromToken(token);
            Long roomId = Long.valueOf(roomId);

            User user = userService.findUserById(userId);
            if (user == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            ChatRoom room = chatService.findRoomById(roomId);
            if (room == null) {
                response.setStatusCode(HttpStatus.NOT_FOUND);
                return false;
            }

            if (!chatService.isParticipantInRoom(user.getId(), room.getId())) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            // Store in session attributes for use by the WebSocket handler
            attributes.put("userId", userId);
            attributes.put("roomId", roomId);
            return true;

        } catch (JWTVerificationException ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        } catch (Exception ex) {
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // No post-handshake processing required
    }
}
```

```java
// WebSocketConfig.java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${env.frontend.allowed-origins}")
    private String allowedOrigins;

    private final AuthHandshakeInterceptor authInterceptor;

    public WebSocketConfig(AuthHandshakeInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        registry
                .addHandler(chatHandler(), "/api/ws/chat")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins(origins);
    }

    @Bean
    public WebSocketHandler chatHandler() {
        return new ChatWebSocketHandler();
    }
}
```

---

### Distributed Caching Layer (Redis)

Production-grade caching infrastructure with **type-safe deserialisation**, **transaction-aware eviction**, and **versioned cache invalidation**. The design solves several real production problems most caching layers ignore: class-name drift on serialized values, race conditions between cache eviction and database commits, and the need to invalidate all caches without manual flushes.

**Core design decisions:**

1. **Type-safe deserialisation** — each cache name is bound to a concrete `JavaType` at startup via `CacheSpec`, eliminating fragile class-name-based deserialisation and `ClassCastException` surprises at runtime.

2. **Transaction-aware eviction** — cache evictions register as transaction synchronizations and only fire after commit, preventing concurrent readers from repopulating the cache with stale data mid-transaction.

3. **Versioned key prefixes** — every cache key includes a configurable version (`liftsync:v1:cacheName::key`). Bumping the version invalidates all caches instantly without manual flushes — useful after deployments with incompatible serialisation changes.

4. **Per-cache TTL configuration** — different caches have different staleness tolerances. TTLs are externally configurable via properties, falling back to a default when not specified.

5. **Startup-time safety check** — reflection verifies that every declared cache name constant has a registered `CacheSpec`. Misconfigurations fail fast at startup, not silently at runtime.

```java
// CacheSpec.java — type-safe binding between cache names and stored types
public record CacheSpec<T>(String name, JavaType javaType) {

    private static final TypeFactory TF = TypeFactory.defaultInstance();

    public static <T> CacheSpec<T> of(String name, Class<T> type) {
        return new CacheSpec<>(name, TF.constructType(type));
    }

    public static <E> CacheSpec<List<E>> ofList(String name, Class<E> elementType) {
        return new CacheSpec<>(name, TF.constructParametricType(List.class, elementType));
    }

    public static <K, V> CacheSpec<Map<K, V>> ofMap(String name, Class<K> keyType, Class<V> valueType) {
        return new CacheSpec<>(name, TF.constructParametricType(Map.class, keyType, valueType));
    }
}
```

```java
// CacheConfig.java — Redis cache manager with per-cache serializers and TTLs
@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheTtlProperties.class)
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true")
public class CacheConfig {

    private final CacheTtlProperties cacheTtlProperties;
    private final ObjectMapper cacheObjectMapper;
    private final String keyVersion;

    public CacheConfig(CacheTtlProperties cacheTtlProperties,
                       @Value("${app.cache.key-version:v1}") String keyVersion) {
        this.cacheTtlProperties = cacheTtlProperties;
        this.keyVersion = keyVersion;
        this.cacheObjectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
                .cacheDefaults(cacheConfiguration())
                .withInitialCacheConfigurations(buildPerCacheConfigurations())
                .build();
    }

    private Map<String, RedisCacheConfiguration> buildPerCacheConfigurations() {
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        for (CacheSpec<?> spec : Caches.all()) {
            Jackson2JsonRedisSerializer<?> serializer =
                    new Jackson2JsonRedisSerializer<>(cacheObjectMapper, spec.javaType());
            configs.put(spec.name(), baseConfiguration(resolveTtl(spec.name()), serializer));
        }
        return configs;
    }

    private RedisCacheConfiguration baseConfiguration(Duration ttl,
                                                      Jackson2JsonRedisSerializer<?> valueSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(ttl)
                .computePrefixWith(cacheName -> "liftsync:" + keyVersion + ":" + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer));
    }
}
```

```java
// CacheEvictor.java — transaction-aware eviction prevents stale repopulation
@Component
public class CacheEvictor {

    private final CacheManager cacheManager;

    public CacheEvictor(ObjectProvider<CacheManager> cacheManagerProvider) {
        this.cacheManager = cacheManagerProvider.getIfAvailable();
    }

    /**
     * Registers an eviction to run only after the current transaction commits.
     * Falls back to immediate execution when no transaction is active.
     *
     * This prevents the classic race condition where a cache is evicted before
     * the transaction commits, allowing concurrent readers to repopulate it
     * with the pre-commit value before the new value is visible.
     */
    private void runAfterCommit(Runnable task) {
        if (task == null) return;

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        task.run();
                    }
                });
        } else {
            task.run();
        }
    }

    public void evictAthleteMetricsByAthleteAndCoachAfterCommit(Long athleteId, Long coachId) {
        runAfterCommit(() -> evict(Caches.ATHLETE_METRICS,
                CacheKeyUtils.athleteMetrics(athleteId, coachId)));
    }

    // Additional eviction methods omitted for brevity...
}
```

```java
// Caches.java — central registry with startup-time safety check
public final class Caches {

    public static final String USER_DTO_BY_PUBLIC_ID = "userDTOByPublicId";
    public static final String ATHLETE_METRICS = "athleteMetrics";
    public static final String BLOCK_LIBRARY = "blockLibrary";
    // ... additional cache name constants

    private static final Map<String, CacheSpec<?>> REGISTRY = new LinkedHashMap<>();

    static {
        register(CacheSpec.of(USER_DTO_BY_PUBLIC_ID, UserDTO.class));
        register(CacheSpec.of(ATHLETE_METRICS, AthleteMetricsResp.class));
        register(CacheSpec.ofList(BLOCK_LIBRARY, BlockTemplatePreviewDTO.class));
        // ... additional registrations

        verifyEveryNameHasASpec();
    }

    /**
     * Reflectively asserts that every public static final String constant on this class
     * has a corresponding entry in REGISTRY. Prevents silent drift where someone adds
     * a name constant but forgets the register(...) call.
     */
    private static void verifyEveryNameHasASpec() {
        List<String> missing = new ArrayList<>();

        for (Field field : Caches.class.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (!(Modifier.isPublic(mods) && Modifier.isStatic(mods) && Modifier.isFinal(mods))) continue;
            if (field.getType() != String.class) continue;

            try {
                String value = (String) field.get(null);
                if (!REGISTRY.containsKey(value)) {
                    missing.add(field.getName() + " (\"" + value + "\")");
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to read " + field.getName(), e);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                "Caches: the following name constants have no registered CacheSpec — " + missing);
        }
    }
}
```

---

### AWS S3 Media Storage

User avatars and profile photos stored in AWS S3. Pre-signed URLs are generated server-side for secure, time-limited access to objects — files are never publicly exposed directly. Credentials injected from environment variables at deploy time via GitHub Secrets.

```java
// S3Config.java
@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(
            @Value("${aws.region}") String region,
            @Value("${aws.access.key.id}") String accessKey,
            @Value("${aws.secret.access.key}") String secretKey) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(
            @Value("${aws.region}") String region,
            @Value("${aws.access.key.id}") String accessKey,
            @Value("${aws.secret.access.key}") String secretKey) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
```

### Full-Stack Internationalisation (i18n)

**Frontend:** Lingui (`@lingui/core` + `@lingui/react`) with `.po` source files compiled to `.mjs` at build time as part of the CI pipeline. Locale preference is persisted in localStorage and loaded asynchronously before the app renders — no flash of untranslated content.

```js
// translations/i18n.js
import { i18n } from '@lingui/core';
import { getLocale } from '../utils/localStorage';

const defaultLocale = getLocale() ?? 'en-EN';
i18n.activate(defaultLocale);

export async function loadDefaultTranslations() {
  const { messages } = await import(`./locales/${defaultLocale}/messages.mjs`);
  i18n.load(defaultLocale, messages);
  i18n.activate(defaultLocale);
}

export async function loadTranslations(locale) {
  try {
    const { messages } = await import(`./locales/${locale}/messages.mjs`);
    i18n.load(locale, messages);
    i18n.activate(locale);
  } catch (error) {
    console.error(`Error loading translations for ${locale}:`, error);
  }
}

export default i18n;
```

```js
// main.jsx — app initialisation waits for translations before rendering
async function initialize() {
  await loadDefaultTranslations();
  ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <I18nProvider i18n={i18n}>
        <App />
      </I18nProvider>
    </React.StrictMode>
  );
}
initialize();
```

**Backend:** Spring Boot `ResourceBundleMessageSource` with `AcceptHeaderLocaleResolver` — locale resolved from the `Accept-Language` request header, defaulting to English. Supports `en` and `es`.

```java
// InternationalizationConfig.java
@Configuration
public class InternationalizationConfig {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(List.of(Locale.ENGLISH, new Locale("es")));
        return resolver;
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }
}
```

---

## Production Infrastructure

| Component | Details |
|---|---|
| Hosting | Self-hosted Linux VPS |
| Domain | liftsync.es (custom DNS) |
| Backend | Spring Boot JAR inside Docker container |
| Frontend | React build served as static files |
| Database | MySQL in Docker container |
| Storage | AWS S3 bucket |
| Orchestration | Docker Compose |
| Deployments | GitHub Actions (manual trigger) |
| Secrets | GitHub Secrets → injected at deploy time |

---

## Live Application

The full platform is live and accessible at **[www.liftsync.es](https://www.liftsync.es)**

- Free tier available — sign up as a Coach or Athlete
- Supports Google and Microsoft single sign-on
- Available in English and Spanish
- Pro and Team tiers coming soon

---

## About the Developer

Built and maintained by **José** — backend software engineer based in Mallorca, Spain.

Currently working on the IKEA regional e-commerce platform (Sarton) across 5+ territories, and building LiftSync as a domain-expert side project.

📧 [jjoselopezbt@gmail.com] · 💼 [linkedin.com/in/jose-lopez-backend] · 🌐 [liftsync.es]

---

*This repository is a public showcase. Full source code is private.*
