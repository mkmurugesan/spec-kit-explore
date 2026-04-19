# Implementation Plan: User Management Setup

**Branch**: `001-user-management-setup`  **Date**: 2026-04-19  **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/001-user-management-setup/spec.md`

---

## Summary

Implement a secure Spring Boot 3.4.4 REST API that provides user registration, authentication (JWT), password reset, and developer-facing admin inspection endpoints. All endpoints are prefixed with `/v1`. Passwords are hashed with BCrypt. Tokens are signed JWTs (JJWT 0.12.6). The admin interface uses HTTP Basic Auth with startup-validated master credentials. PostgreSQL 16 is the persistence layer managed by Liquibase changelog migrations. The application and database are orchestrated via `docker-compose.yml` at the project root. No automated testing of any kind is included.

---

## Technical Context

**Language/Version**: Java 21 LTS (Eclipse Temurin)  
**Framework**: Spring Boot **3.4.4** (latest stable as of 2026-04-19)  
**Build Tool**: Maven **3.9.9** (latest stable)  
**Primary Dependencies**:

| Dependency | Starter / Artifact | Justification |
|---|---|---|
| Spring Web | `spring-boot-starter-web` | REST controllers, HTTP layer |
| Spring Data JPA | `spring-boot-starter-data-jpa` | Repository layer, ORM (Hibernate 6) |
| Spring Security | `spring-boot-starter-security` | JWT filter chain, HTTP Basic Auth for admin |
| Spring Validation | `spring-boot-starter-validation` | `@Valid`, `@Email`, `@Size` on request DTOs |
| PostgreSQL JDBC | `postgresql` (Boot-managed) | Database driver |
| Liquibase Core | `liquibase-core` (Boot-managed) | Versioned changelog-based schema migrations |
| JJWT API | `io.jsonwebtoken:jjwt-api:0.12.6` | JWT creation & validation |
| JJWT Impl | `io.jsonwebtoken:jjwt-impl:0.12.6` | JJWT runtime |
| JJWT Jackson | `io.jsonwebtoken:jjwt-jackson:0.12.6` | JWT JSON serialisation |
| Lombok | `lombok` (Boot-managed) | Reduce boilerplate (getters/setters/builders) |

**Storage**: PostgreSQL 16 (Docker image `postgres:16-alpine`)  
**Testing**: **Prohibited** by constitution (manual validation only — no JUnit, Mockito, Spring Boot Test, or any test framework)  
**Target Platform**: Linux container / JVM backend  
**Project Type**: Spring Boot backend REST API  
**API Versioning**: URI prefix `/v1` on all endpoints  
**Performance Goals**: < 1 s response time for individual requests on local hardware (SC-007)  
**Constraints**: No frontend/UI; no test artifacts; no test directories; minimal dependencies; latest Spring Boot + Maven  
**Scale/Scope**: Local/dev deployment; handful of REST endpoints; single-instance

---

## Constitution Check

*GATE: All items confirmed before Phase 0 research. Re-checked post-design — all pass.*

- [x] **Latest Spring Boot**: Confirmed version `3.4.4` is the latest stable release at implementation time (see `research.md` §1).
- [x] **Latest Maven**: Confirmed version `3.9.9` is the latest stable release at implementation time (see `research.md` §3).
- [x] **Layered architecture**: Design follows controller → service → repository boundaries; no cross-layer shortcuts. Controllers handle HTTP only; services own business logic; repositories handle persistence.
- [x] **Minimal dependencies**: Every listed dependency above has a documented justification in this plan and in `research.md`. No dependency added for trivial utility.
- [x] **No test frameworks**: Dependency list contains NO JUnit, Mockito, Testcontainers, Spring Boot Test, or any other test/assertion library.
- [x] **No test artifacts**: No `src/test/` directories, test classes, test configuration, or test pipeline steps are planned or created.
- [x] **Backend only**: No frontend, mobile, or UI framework is referenced anywhere in this plan.

---

## Project Structure

### Documentation (this feature)

```text
specs/001-user-management-setup/
├── plan.md              # This file (/speckit.plan output)
├── spec.md              # Feature specification
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── auth-contract.md
│   └── admin-contract.md
└── checklists/
    └── requirements.md
```

### Source Code (repository root)

```text
.
├── docker-compose.yml                          # PostgreSQL + Spring Boot orchestration
├── Dockerfile                                  # Multi-stage: Maven build → JRE runtime
├── pom.xml                                     # Maven build — Spring Boot 3.4.4 parent
└── src/
    └── main/
        ├── java/
        │   └── com/example/usermanagement/
        │       ├── UserManagementApplication.java      # @SpringBootApplication entry point
        │       ├── config/
        │       │   ├── SecurityConfig.java             # Spring Security filter chain
        │       │   ├── AdminCredentialsValidator.java  # @PostConstruct startup guard
        │       │   └── JwtProperties.java              # @ConfigurationProperties for JWT/token expiry
        │       ├── controller/
        │       │   ├── AuthController.java             # POST /v1/api/auth/**
        │       │   └── AdminUserController.java        # GET /v1/api/admin/users/**
        │       ├── service/
        │       │   ├── UserService.java                # Registration, sign-in, password logic
        │       │   ├── TokenService.java               # JWT creation & validation
        │       │   └── PasswordResetService.java       # Reset token lifecycle
        │       ├── repository/
        │       │   ├── UserRepository.java             # Spring Data JPA — users table
        │       │   └── PasswordResetTokenRepository.java  # Spring Data JPA — password_reset_tokens
        │       ├── model/
        │       │   ├── User.java                       # @Entity — users table
        │       │   ├── UserStatus.java                 # Enum: ACTIVE, INACTIVE
        │       │   └── PasswordResetToken.java         # @Entity — password_reset_tokens table
        │       ├── dto/
        │       │   ├── SignupRequest.java
        │       │   ├── SignupResponse.java
        │       │   ├── SigninRequest.java
        │       │   ├── SigninResponse.java
        │       │   ├── PasswordResetRequestDto.java
        │       │   ├── PasswordResetConfirmDto.java
        │       │   └── UserSummaryDto.java             # Admin list/detail response (no hash)
        │       └── exception/
        │           ├── GlobalExceptionHandler.java     # @RestControllerAdvice
        │           ├── EmailAlreadyInUseException.java
        │           ├── InvalidCredentialsException.java
        │           └── ResourceNotFoundException.java
            └── resources/
            ├── application.properties                  # All configurable properties
            └── db/changelog/
                ├── db.changelog-master.yaml
                ├── changes/
                │   ├── 001-create-users-table.sql
                │   └── 002-create-password-reset-tokens-table.sql
```

**Structure Decision**: Standard Spring Boot Maven layout. Single Maven module (no multi-module); clean package-per-concern under `com/example/usermanagement`. No `src/test/` directory — constitution Principle IV.

---

## Complexity Tracking

No constitution violations. All items resolved within standard constraints.

---

## Phase 0: Research

**Status**: ✅ Complete — see [`research.md`](./research.md)

### Key Decisions Resolved

| Question | Resolution |
|---|---|
| Spring Boot version | 3.4.4 (latest stable) |
| Java version | 21 LTS |
| Maven version | 3.9.9 |
| JWT library | JJWT 0.12.6 |
| Password hashing | BCrypt (Spring Security built-in — no extra dependency) |
| Schema migration | Liquibase Core (managed by Spring Boot BOM) |
| Database | PostgreSQL 16 (Docker `postgres:16-alpine`) |
| Admin credential guard | `@PostConstruct` fail-fast on blank values |
| API versioning | URI prefix `/v1` |
| Token expiry config | `@ConfigurationProperties` beans |

---

## Phase 1: Design

**Status**: ✅ Complete

### Artifacts Produced

| Artifact | Location | Description |
|---|---|---|
| Data Model | [`data-model.md`](./data-model.md) | Entity definitions, table DDL, indexes, state transitions |
| Auth Contract | [`contracts/auth-contract.md`](./contracts/auth-contract.md) | Signup, signin, password reset request/confirm endpoints |
| Admin Contract | [`contracts/admin-contract.md`](./contracts/admin-contract.md) | Admin list users, get user by ID endpoints |
| Quickstart | [`quickstart.md`](./quickstart.md) | How to run locally with Docker Compose, manual API walkthrough |
| Docker Compose | [`/docker-compose.yml`](../../docker-compose.yml) | PostgreSQL + Spring Boot service definitions |

---

## Implementation Notes

### Security Architecture

```
Request
  │
  ▼
Spring Security Filter Chain
  ├── /v1/api/admin/**  → BasicAuthenticationFilter → AdminBasicAuthProvider
  ├── /v1/api/auth/**   → permitAll (public endpoints)
  └── other protected   → JwtAuthenticationFilter → validates Bearer token
```

- **Admin endpoints** use a dedicated `BasicAuthenticationFilter` entry point backed by a custom `AuthenticationProvider` that compares against `admin.master.username` / `admin.master.password` loaded from properties.
- **User JWT endpoints** use a custom `OncePerRequestFilter` that extracts the `Authorization: Bearer <token>` header, validates the JWT signature and expiry, and sets the `SecurityContext`.
- Admin and user auth are on **separate security filter chains** (Spring Security 6 supports multiple `SecurityFilterChain` beans ordered by `@Order`).

### JWT Design

- Algorithm: HMAC-SHA256 (`HS256`)
- Claims: `sub` (user UUID), `iat`, `exp`
- Signing key: loaded from `JWT_SECRET` environment variable (must be ≥ 256 bits / 32 bytes)
- Access token: default expiry 15 min (`jwt.access-token.expiry=15m`)
- Refresh token: default expiry 7 days (`jwt.refresh-token.expiry=7d`)
- Refresh token use (obtaining new access token) is a post-v1 enhancement; token is issued at sign-in for client storage

### Password Reset Token Design

- Generated via `SecureRandom` (32 bytes → 64 hex chars)
- Stored in `password_reset_tokens` table with `expires_at` and `used` flag
- **Invalidation**: service executes `UPDATE ... SET used=true WHERE user_id=? AND used=false AND expires_at > NOW()` before inserting new token
- **Dev mode detection**: `app.env=dev` property; if `dev`, `resetToken` is included in the response body

### Admin Credential Validation at Startup

```java
@Configuration
public class AdminCredentialsValidator {
    @Value("${admin.master.username:}") private String username;
    @Value("${admin.master.password:}") private String password;

    @PostConstruct
    public void validate() {
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                "admin.master.username and admin.master.password must be configured");
        }
    }
}
```

### Liquibase Migrations

```
db/changelog/db.changelog-master.yaml         — master changelog (includes all changesets)
db/changelog/changes/001-create-users-table.sql
db/changelog/changes/002-create-password-reset-tokens-table.sql
```

**Master changelog** (`db.changelog-master.yaml`):
```yaml
databaseChangeLog:
  - includeAll:
      path: db/changelog/changes/
      relativeToChangelogFile: false
```

Full DDL documented in [`data-model.md`](./data-model.md).

### application.properties (template)

```properties
# Server
server.port=8080

# Database
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/userdb}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:userapp}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:userapp_pass}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Liquibase
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml

# JWT
jwt.secret=${JWT_SECRET:change-me-in-production-min-256-bit-key-abcdefghijklmnop}
jwt.access-token.expiry=${JWT_ACCESS_TOKEN_EXPIRY:15m}
jwt.refresh-token.expiry=${JWT_REFRESH_TOKEN_EXPIRY:7d}

# Password Reset
auth.password-reset.token.expiry=${AUTH_PASSWORD_RESET_TOKEN_EXPIRY:1h}

# Admin Credentials (application MUST fail to start if blank)
admin.master.username=${ADMIN_MASTER_USERNAME:}
admin.master.password=${ADMIN_MASTER_PASSWORD:}

# App environment (dev = return reset token in response body)
app.env=${APP_ENV:dev}
```

### Dockerfile (Multi-stage)

```dockerfile
# Stage 1 — Build
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2 — Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

> `-DskipTests` is used only because there are no tests; this is consistent with constitution Principle IV.

---

## Manual Validation Plan

All validation is done manually via curl or Postman per constitution Principle IV and spec SC-001 through SC-008.

| Scenario | Method | Expected Result |
|---|---|---|
| Sign up with valid data | POST `/v1/api/auth/signup` | 201 + user summary (no passwordHash) |
| Sign up with duplicate email | POST `/v1/api/auth/signup` | 409 Conflict |
| Sign up with short password | POST `/v1/api/auth/signup` | 400 Validation error |
| Sign in with correct credentials | POST `/v1/api/auth/signin` | 200 + accessToken + refreshToken |
| Sign in with wrong password | POST `/v1/api/auth/signin` | 401 (no email leak) |
| Password reset request (dev) | POST `/v1/api/auth/password-reset/request` | 200 + resetToken in body |
| Password reset confirm (valid token) | POST `/v1/api/auth/password-reset/confirm` | 200 confirmation |
| Password reset confirm (expired/used token) | POST `/v1/api/auth/password-reset/confirm` | 400 error |
| Second reset request invalidates first | Request new token, attempt old | Old token → 400 |
| Admin list users (correct creds) | GET `/v1/api/admin/users` | 200 + user array |
| Admin list users (wrong creds) | GET `/v1/api/admin/users` | 401 |
| Admin get user by ID (exists) | GET `/v1/api/admin/users/{id}` | 200 + user detail |
| Admin get user by ID (not found) | GET `/v1/api/admin/users/{id}` | 404 |
| Admin endpoint with user JWT | GET `/v1/api/admin/users` (Bearer token) | 401 |
| App start without admin credentials | Remove env vars, restart | Startup failure + `IllegalStateException` |
| Inspect DB after signup | `SELECT password_hash FROM users` | Only bcrypt hash present (no plaintext) |

Full step-by-step curl commands are in [`quickstart.md`](./quickstart.md).

---

## Next Steps

- [ ] **Phase 2**: Run `/speckit.tasks` to generate `tasks.md` with implementation task breakdown
- [ ] Implement `pom.xml` with all declared dependencies
- [ ] Implement Liquibase changelog SQL files (DDL from `data-model.md`)
- [ ] Implement entities, repositories, services, controllers, DTOs, and exception handlers per project structure above
- [ ] Write `Dockerfile` (multi-stage, above)
- [ ] Validate all endpoints manually per the validation plan above
- [ ] Capture curl output as evidence in feature docs (SC-001 through SC-008)

