# LiftSync — Fitness Coaching Platform

> A full-stack SaaS platform for fitness coaches to manage clients, routines, nutrition plans, and progress tracking. Built end-to-end by a single developer with a production deployment pipeline on a self-hosted Linux VPS.

🌐 **Live at [www.liftsync.es](https://www.liftsync.es)** · Private beta · English & Spanish

---

## What is LiftSync?

LiftSync bridges the gap between fitness coaches and their athletes. Coaches manage client onboarding, assign personalised workout routines and nutrition plans, automate weekly check-in questionnaires, and track client progress over time — all from one platform.

This repository is a **public showcase** of the system architecture, CI/CD pipeline, and key technical implementations. The full source code is private to protect business logic.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                      CLIENT LAYER                        │
│                                                         │
│   React Web App (i18n EN/ES)    React Native Mobile     │
│         ↓                              ↓                │
└─────────────────────────────────────────────────────────┘
                          │
                    REST API (HTTPS)
                          │
┌─────────────────────────────────────────────────────────┐
│                    BACKEND LAYER                         │
│                                                         │
│              Java 17 + Spring Boot 3                    │
│                                                         │
│   ┌──────────┐  ┌──────────┐  ┌──────────────────────┐ │
│   │  Auth    │  │ Business │  │    i18n / Locale     │ │
│   │ OAuth2   │  │  Logic   │  │  MessageSource       │ │
│   │ JWT      │  │  Layer   │  │  LocaleContextHolder │ │
│   └──────────┘  └──────────┘  └──────────────────────┘ │
│                                                         │
└─────────────────────────────────────────────────────────┘
                          │
        ┌─────────────────┼──────────────────┐
        ↓                 ↓                  ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
│    MySQL     │  │   AWS S3     │  │  OAuth2 Providers │
│  Database   │  │ Photos/Media │  │  Google/Microsoft │
│             │  │ (avatars +   │  └──────────────────┘
│             │  │ chat media)  │
└──────────────┘  └──────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3, Spring Security |
| Auth | OAuth2 (Google + Microsoft), JWT |
| Database | MySQL |
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
liftsync-api-showcase/
│
├── .github/
│   └── workflows/
│       ├── deploy-backend.yml      # Backend CI/CD pipeline
│       └── deploy-frontend.yml     # Frontend CI/CD pipeline
│
├── backend/
│   ├── src/main/java/es/liftsync/
│   │   ├── LiftSyncApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java         # Spring Security + OAuth2
│   │   │   ├── S3Config.java               # AWS S3 client configuration
│   │   │   └── MessageSourceConfig.java    # i18n configuration
│   │   ├── controller/
│   │   │   └── ExampleController.java      # Example REST endpoint
│   │   └── i18n/
│   │       └── LocaleConfig.java           # LocaleContextHolder setup
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── i18n/
│   │   │   ├── config.ts                   # react-i18next setup
│   │   │   └── locales/
│   │   │       ├── en/messages.po          # English translations
│   │   │       └── es/messages.po          # Spanish translations
│   │   └── components/
│   │       └── ExampleComponent.jsx        # Example i18n component
│   └── package.json
│
├── docker-compose.yml              # Production orchestration
├── docker-compose.dev.yml          # Local development
├── Makefile                        # Developer shortcuts
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
│  SCP JAR → VPS   │
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

        if (token == null || roomIdS == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Long userId = tokenService.verifyAndGetIdFromToken(token);
            Long roomId = Long.valueOf(roomIdS);

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

        } catch (JWTVerificationException | NumberFormatException ex) {
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

📧 [jjoselopezbt@email.com] · 💼 [linkedin.com/in/jose-lopez-backend] · 🌐 [liftsync.es]

---

*This repository is a public showcase. Full source code is private.*
