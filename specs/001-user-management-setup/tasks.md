---
description: "Task list for User Management Setup feature"
---

# Tasks: User Management Setup

**Input**: Design documents from `/specs/001-user-management-setup/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/auth-contract.md, contracts/admin-contract.md, research.md

**Tests**: No test tasks — constitution prohibits all automated testing.

**Organization**: Tasks are grouped by user story to enable independent implementation and manual validation of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Exact file paths included in every task description

---

## Phase 1: Setup (Project Initialization)

**Purpose**: Bootstrap the Maven project, Docker infrastructure, and Spring Boot entry point so the application can build and run locally.

- [x] T001 Create `pom.xml` at project root with Spring Boot 3.4.4 parent, Java 21, and dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `liquibase-core`, `postgresql` driver, `jjwt-api 0.12.6`, `jjwt-impl 0.12.6`, `jjwt-jackson 0.12.6`, `lombok` — **do NOT add `spring-boot-starter-mail`** (email is out of scope for v1)
- [x] T002 [P] Create `src/main/java/com/example/usermanagement/UserManagementApplication.java` — Spring Boot `@SpringBootApplication` entry point
- [x] T003 [P] Create `Dockerfile` at project root — multi-stage Maven build targeting Java 21 runtime, exposing port 8080
- [x] T004 [P] Create `src/main/resources/application.properties` with datasource URL (`spring.datasource.url=jdbc:postgresql://localhost:5432/userdb`), JPA settings (`spring.jpa.hibernate.ddl-auto=validate`), Liquibase enabled (`spring.liquibase.enabled=true`, `spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml`), JWT properties (`jwt.secret`, `jwt.access-token.expiry=15m`, `jwt.refresh-token.expiry=7d`), password reset expiry (`auth.password-reset.token.expiry=1h`), admin credentials (`admin.master.username=`, `admin.master.password=`), and app environment (`app.env=dev`)

**Checkpoint**: `mvn package -DskipTests` succeeds and `docker-compose up` starts the PostgreSQL container.

---

## Phase 2: Foundational Infrastructure (Blocking Prerequisites)

**Purpose**: Core entities, repositories, Liquibase schema, exception handling, JWT config, and Security skeleton that ALL user stories depend on. No user story can begin until this phase is complete.

**⚠️ CRITICAL**: Complete and verify this phase before starting any user story phase.

- [x] T005 Create Liquibase master changelog `src/main/resources/db/changelog/db.changelog-master.yaml` that includes `changes/001-create-users-table.sql` and `changes/002-create-password-reset-tokens-table.sql`
- [x] T006 [P] Create `src/main/resources/db/changelog/changes/001-create-users-table.sql` — DDL for `users` table with columns: `id` (UUID PK), `email` (unique not null), `password_hash` (not null), `first_name`, `last_name`, `status` (varchar, not null, default `'ACTIVE'`), `role` (varchar, not null, default `'USER'`), `created_at`, `updated_at`
- [x] T007 [P] Create `src/main/resources/db/changelog/changes/002-create-password-reset-tokens-table.sql` — DDL for `password_reset_tokens` table with columns: `id` (UUID PK), `user_id` (FK → users.id), `token` (unique not null), `expires_at` (not null), `used` (boolean default false), `created_at`
- [x] T008 [P] Create `src/main/java/com/example/usermanagement/model/UserStatus.java` — enum with values `ACTIVE`, `INACTIVE` only (per spec — no SUSPENDED in v1)
- [x] T009 [P] Create `src/main/java/com/example/usermanagement/model/User.java` — JPA `@Entity` mapped to `users` table; fields: `id` (UUID), `email`, `passwordHash`, `firstName`, `lastName`, `status` (UserStatus enum), `role` (String), `createdAt`, `updatedAt`; annotate with `@PrePersist`/`@PreUpdate` for timestamps
- [x] T010 [P] Create `src/main/java/com/example/usermanagement/model/PasswordResetToken.java` — JPA `@Entity` mapped to `password_reset_tokens`; fields: `id` (UUID), `user` (`@ManyToOne` → User), `token`, `expiresAt`, `used`, `createdAt`
- [x] T011 [P] Create `src/main/java/com/example/usermanagement/repository/UserRepository.java` — `JpaRepository<User, UUID>` with `Optional<User> findByEmail(String email)` and `boolean existsByEmail(String email)`
- [x] T012 [P] Create `src/main/java/com/example/usermanagement/repository/PasswordResetTokenRepository.java` — `JpaRepository<PasswordResetToken, UUID>` with `Optional<PasswordResetToken> findByToken(String token)`; add `@Modifying @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.user = :user AND t.used = false AND t.expiresAt > :now") void markAllActiveAsUsedByUser(@Param("user") User user, @Param("now") Instant now)` — use UPDATE (not delete) to preserve audit trail
- [x] T013 [P] Create `src/main/java/com/example/usermanagement/config/JwtProperties.java` — `@ConfigurationProperties(prefix = "jwt")` binding `secret` (String), `accessTokenExpiry` (Duration mapped from `jwt.access-token.expiry`), and `refreshTokenExpiry` (Duration mapped from `jwt.refresh-token.expiry`); create separate `@ConfigurationProperties(prefix = "auth.password-reset.token")` bean or inner class binding `expiry` (Duration); annotate with `@Configuration` and `@EnableConfigurationProperties`
- [x] T014 [P] Create `src/main/java/com/example/usermanagement/exception/EmailAlreadyInUseException.java` — `RuntimeException` subclass
- [x] T015 [P] Create `src/main/java/com/example/usermanagement/exception/InvalidCredentialsException.java` — `RuntimeException` subclass
- [x] T016 [P] Create `src/main/java/com/example/usermanagement/exception/ResourceNotFoundException.java` — `RuntimeException` subclass
- [x] T017 Create `src/main/java/com/example/usermanagement/exception/GlobalExceptionHandler.java` — `@RestControllerAdvice` handling `EmailAlreadyInUseException` (409), `InvalidCredentialsException` (401), `ResourceNotFoundException` (404), `MethodArgumentNotValidException` (400 with field errors), and generic `Exception` (500); all responses as JSON `{ "error": "...", "message": "..." }`
- [x] T018 Create `src/main/java/com/example/usermanagement/config/SecurityConfig.java` — `@Configuration` `@EnableWebSecurity` `@EnableMethodSecurity`; declare `PasswordEncoder` bean (`BCryptPasswordEncoder`); declare `AuthenticationManager` bean; define `SecurityFilterChain` bean with CSRF disabled, stateless session, explicitly permitting `/v1/api/auth/**` (public) and requiring authentication for all other paths; `UserDetailsService` bean that loads User by email from `UserRepository` — JWT filter wired in Phase 4

**Checkpoint**: Application starts, Liquibase runs both changelogs, tables exist in PostgreSQL, `/actuator/health` or app logs confirm clean startup.

---

## Phase 3: User Story 1 — User Sign-Up (Priority: P1) 🎯 MVP

**Goal**: A new visitor can register with email + password and receive a JWT token confirming account creation.

**Independent Validation**: `POST /v1/api/auth/signup` with `{"email":"a@b.com","password":"Test1234!","firstName":"Ada","lastName":"L"}` returns HTTP 201 with a `token` field. Re-posting the same email returns HTTP 409.

### Implementation

- [x] T019 [P] [US1] Create `src/main/java/com/example/usermanagement/dto/SignupRequest.java` — record/class with fields `firstName` (`@NotBlank`), `lastName` (`@NotBlank`), `email` (`@Email @NotBlank`), `password` (`@NotBlank @Size(min=8)`)
- [x] T020 [P] [US1] Create `src/main/java/com/example/usermanagement/dto/SignupResponse.java` — record/class with fields `id` (UUID), `firstName` (String), `lastName` (String), `email` (String), `status` (String), `createdAt` (Instant) — **no token field** (signup does not return a JWT per auth-contract)
- [x] T021 [US1] Create `src/main/java/com/example/usermanagement/service/UserService.java` — `@Service`; inject `UserRepository`, `PasswordEncoder`; implement `SignupResponse register(SignupRequest request)`: check `existsByEmail` (throw `EmailAlreadyInUseException` if true), encode password, set role `USER` and status `ACTIVE`, set `createdAt`, save User, return `SignupResponse` mapped from saved entity (`id`, `firstName`, `lastName`, `email`, `status`, `createdAt`) — no token in response
- [x] T022 [US1] Create `src/main/java/com/example/usermanagement/controller/AuthController.java` — `@RestController @RequestMapping("/v1/api/auth")`; inject `UserService`; implement `POST /signup` accepting `@Valid @RequestBody SignupRequest`, returning `ResponseEntity<SignupResponse>` with HTTP 201

**Checkpoint**: `POST /v1/api/auth/signup` returns 201 with JSON body `{id, firstName, lastName, email, status, createdAt}`; duplicate email returns 409.

---

## Phase 4: User Story 2 — User Sign-In (Priority: P1)

**Goal**: A registered user can sign in with email + password and receive a valid JWT for subsequent requests.

**Independent Validation**: `POST /v1/api/auth/signin` with valid credentials returns HTTP 200 with `token`; invalid credentials return 401. Using the returned token as `Authorization: Bearer <token>` on a protected endpoint is accepted.

### Implementation

- [x] T023 [P] [US2] Create `src/main/java/com/example/usermanagement/dto/SigninRequest.java` — record/class with `email` (`@Email @NotBlank`) and `password` (`@NotBlank`)
- [x] T024 [P] [US2] Create `src/main/java/com/example/usermanagement/dto/SigninResponse.java` — record/class with fields `accessToken` (String), `refreshToken` (String), `tokenType` (String, always `"Bearer"`), `accessTokenExpiresIn` (long, seconds), `refreshTokenExpiresIn` (long, seconds)
- [x] T025 [US2] Create `src/main/java/com/example/usermanagement/service/TokenService.java` — `@Service`; inject `JwtProperties`; implement `String generateAccessToken(User user)` using JJWT 0.12.6 (`Jwts.builder()`, signing with HMAC-SHA256, subject = user email, expiry from `jwt.access-token.expiry`); implement `String generateRefreshToken(User user)` with expiry from `jwt.refresh-token.expiry`; implement `String extractEmail(String token)` and `boolean isTokenValid(String token)`
- [x] T026 [US2] Update `src/main/java/com/example/usermanagement/service/UserService.java` — inject `TokenService`; implement `SigninResponse signin(SigninRequest request)`: load user by email (throw `InvalidCredentialsException` if absent), verify password with `PasswordEncoder.matches` (throw `InvalidCredentialsException` if mismatch), call `TokenService.generateAccessToken` and `TokenService.generateRefreshToken`, return `SigninResponse` with `accessToken`, `refreshToken`, `tokenType="Bearer"`, `accessTokenExpiresIn` (seconds from jwt.access-token.expiry), `refreshTokenExpiresIn` (seconds from jwt.refresh-token.expiry)
- [x] T027 [US2] Update `src/main/java/com/example/usermanagement/controller/AuthController.java` — add `POST /signin` endpoint accepting `@Valid @RequestBody SigninRequest`, returning `ResponseEntity<SigninResponse>` HTTP 200
- [x] T028 [US2] Create JWT request filter `src/main/java/com/example/usermanagement/config/JwtAuthFilter.java` — `OncePerRequestFilter`; extract `Authorization: Bearer <token>` header; validate via `TokenService.isTokenValid`; set `UsernamePasswordAuthenticationToken` in `SecurityContextHolder` when valid
- [x] T029 [US2] Update `src/main/java/com/example/usermanagement/config/SecurityConfig.java` — add `JwtAuthFilter` before `UsernamePasswordAuthenticationFilter` in the filter chain; implement `UserDetailsService` bean that loads User by email from `UserRepository` (user's `role` field maps to Spring Security `GrantedAuthority` e.g. `ROLE_USER`, `ROLE_ADMIN`)

**Checkpoint**: `POST /v1/api/auth/signin` returns 200 with JWT; invalid credentials return 401; Bearer token accepted on protected routes.

---

## Phase 5: User Story 3 — Password Reset (Priority: P2)

**Goal**: A user who forgot their password can request a reset link and set a new password via a token-secured confirm step.

**Independent Validation**: `POST /v1/api/auth/password-reset/request` with a registered email returns 200. `POST /v1/api/auth/password-reset/confirm` with the token + new password returns 200. Subsequent sign-in with the new password succeeds.

### Implementation

- [x] T030 [P] [US3] Create `src/main/java/com/example/usermanagement/dto/PasswordResetRequestDto.java` — record/class with `email` (`@Email @NotBlank`)
- [x] T031 [P] [US3] Create `src/main/java/com/example/usermanagement/dto/PasswordResetConfirmDto.java` — record/class with `token` (`@NotBlank`) and `newPassword` (`@NotBlank @Size(min=8)`)
- [x] T032 [US3] Create `src/main/java/com/example/usermanagement/service/PasswordResetService.java` — `@Service`; inject `UserRepository`, `PasswordResetTokenRepository`, `PasswordEncoder`; implement `String requestReset(String email)`: look up User by email (return null/empty silently if not found — prevents enumeration), call `passwordResetTokenRepository.markAllActiveAsUsedByUser(user, Instant.now())` to invalidate previous tokens, generate token using `SecureRandom` (32 bytes → 64 hex chars via `HexFormat`), persist `PasswordResetToken` with `expiresAt = now + auth.password-reset.token.expiry` (1h default), return token string (caller decides whether to include in response based on `app.env`); implement `void confirmReset(String token, String newPassword)`: find `PasswordResetToken` by token value (throw `ResourceNotFoundException` if absent), verify `!used && expiresAt.isAfter(Instant.now())` (throw `InvalidCredentialsException` if invalid), encode new password, save user, mark token `used=true`, save token — **no JavaMailSender; no email sending in v1**
- [x] T033 [US3] Update `src/main/java/com/example/usermanagement/controller/AuthController.java` — inject `PasswordResetService` and `@Value("${app.env:dev}") String appEnv`; add `POST /password-reset/request` (body: `PasswordResetRequestDto`, calls `passwordResetService.requestReset(email)`, if `appEnv.equals("dev")` include `resetToken` in response body, otherwise return generic message only); add `POST /password-reset/confirm` (body: `PasswordResetConfirmDto`, calls `passwordResetService.confirmReset(token, newPassword)`, response: 200 `{"message": "Password updated successfully."}`)

**Checkpoint**: Full reset flow works end-to-end; expired or reused tokens return 400/401.

---

## Phase 6: User Story 4 — Admin User Management APIs (Priority: P3)

**Goal**: An admin (configured via environment/properties) can list all users, view a user's detail, and update a user's status via authenticated admin-only endpoints.

**Independent Validation**: `GET /v1/api/admin/users` with `admin.master.*` Basic Auth credentials returns 200 with user list. `GET /v1/api/admin/users/{id}` returns 200 for existing user and 404 for unknown ID. Non-admin credentials or missing header returns 401.

### Implementation

- [x] T034 [P] [US4] Create `src/main/java/com/example/usermanagement/dto/UserSummaryDto.java` — record/class with `id` (UUID), `firstName` (String), `lastName` (String), `email` (String), `status` (String), `role` (String), `createdAt` (Instant) — **no passwordHash field ever** (FR-013)
- [x] T035 [P] [US4] Create `src/main/java/com/example/usermanagement/config/AdminCredentialsValidator.java` — `@Configuration`; inject `@Value("${admin.master.username:}") String username` and `@Value("${admin.master.password:}") String password`; implement `@PostConstruct void validate()` that throws `IllegalStateException("admin.master.username and admin.master.password must be configured")` if either is blank — satisfies FR-012a and SC-008
- [x] T036 [US4] Create `src/main/java/com/example/usermanagement/controller/AdminUserController.java` — `@RestController @RequestMapping("/v1/api/admin")`; protect with HTTP Basic Auth via Spring Security (admin endpoints use a separate `SecurityFilterChain` with `httpBasic()` authentication against `admin.master.username`/`admin.master.password`); inject `UserRepository`; implement:
  - `GET /users` — return `List<UserSummaryDto>` (map all User entities; no pagination required in v1)
  - `GET /users/{id}` — return `UserSummaryDto` for given UUID (throw `ResourceNotFoundException` if absent → 404)
  - Both methods MUST log a WARN-level message on any unauthorised access attempt (FR-014)
- [x] T037 [US4] Update `src/main/java/com/example/usermanagement/config/SecurityConfig.java` — add a second `@Bean @Order(1) SecurityFilterChain adminFilterChain(HttpSecurity)` that matches `/v1/api/admin/**`, uses `httpBasic()`, and authenticates against a custom `UserDetailsService` that validates against `admin.master.username` / `admin.master.password` from properties; the existing JWT filter chain becomes `@Order(2)` for all other paths; add `@Bean` for the admin `UserDetailsService` reading from those properties — also add WARN-level logging for 401 responses via `AuthenticationFailureHandler`
- [x] T038 [US4] Update `src/main/resources/application.properties` — add placeholder entries:
  ```properties
  # Admin master credentials — MUST be overridden before any non-local deployment
  admin.master.username=${ADMIN_MASTER_USERNAME:dev-admin}
  admin.master.password=${ADMIN_MASTER_PASSWORD:D3v@dm1n!S3cur3}
  # App environment — controls whether reset token appears in response body
  app.env=${APP_ENV:dev}
  ```

**Checkpoint**: Admin `GET /v1/api/admin/users` returns 200 with `admin.master.*` Basic Auth credentials; returns 401 for wrong credentials, missing header, or user JWT; 401 attempts are logged at WARN level; app refuses to start if credentials are absent.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final wiring, hardening, and evidence documentation.

- [x] T039 [P] Review and finalize `Dockerfile` at project root — confirm multi-stage build copies the correct fat JAR (`target/*.jar`), sets `ENTRYPOINT`, and exposes port 8080
- [x] T040 [P] Review `docker-compose.yml` at project root — confirm PostgreSQL service is named `db`, uses `postgres:16-alpine` image, exposes port 5432, has `POSTGRES_DB=userdb` env var (matching `application.properties` datasource URL `jdbc:postgresql://db:5432/userdb`), and has a named volume for data persistence
- [x] T041 [P] Add `spring.jpa.open-in-view=false` and connection pool settings (`spring.datasource.hikari.maximum-pool-size=10`) to `src/main/resources/application.properties`
- [ ] T042 Perform full manual validation end-to-end per `specs/001-user-management-setup/quickstart.md` — capture `curl` request/response evidence for all four user stories in `specs/001-user-management-setup/docs/validation-evidence.md`
- [ ] T043 Update `README.md` at project root — add section for User Management Setup: local run instructions (`docker-compose up`, `mvn spring-boot:run`), environment variable table, and sample curl commands for all endpoints

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — **BLOCKS all user story phases**
- **Phase 3 (US1 Sign-Up)**: Depends on Phase 2
- **Phase 4 (US2 Sign-In)**: Depends on Phase 3 (TokenService integrates into UserService)
- **Phase 5 (US3 Password Reset)**: Depends on Phase 2 — can proceed after Foundational independently of US2
- **Phase 6 (US4 Admin)**: Depends on Phase 4 (JWT + role system must be complete)
- **Phase 7 (Polish)**: Depends on all desired user story phases

### User Story Dependencies

| Story | Depends On | Can Parallel With |
|-------|-----------|-------------------|
| US1 Sign-Up | Phase 2 | US3 (after Phase 2) |
| US2 Sign-In | US1 (TokenService wires into UserService) | — |
| US3 Password Reset | Phase 2 | US1 (after Phase 2) |
| US4 Admin APIs | US2 (needs JWT + role) | — |

### Within Each Phase

1. DTOs (can be parallel)
2. Models / Entities (can be parallel)
3. Services (depend on repositories and models)
4. Controllers (depend on services)
5. Security config updates (depend on services/filters)

---

## Parallel Execution Examples

### Phase 2 Parallel Launch (Foundational)

```
Task T005 — Liquibase master changelog
Parallel group A (run together):
  T006 — 001-create-users-table.sql
  T007 — 002-create-password-reset-tokens-table.sql
  T008 — UserStatus.java
  T009 — User.java
  T010 — PasswordResetToken.java
  T011 — UserRepository.java
  T012 — PasswordResetTokenRepository.java
  T013 — JwtProperties.java
  T014 — EmailAlreadyInUseException.java
  T015 — InvalidCredentialsException.java
  T016 — ResourceNotFoundException.java
Then sequentially:
  T017 — GlobalExceptionHandler.java
  T018 — SecurityConfig.java (skeleton)
```

### Phase 3 Parallel Launch (US1)

```
Parallel group (run together):
  T019 — SignupRequest.java
  T020 — SignupResponse.java
Then sequentially:
  T021 — UserService.java (register)
  T022 — AuthController.java (signup endpoint)
```

### Phase 5 Parallel Launch (US3 — can start after Phase 2)

```
Parallel group:
  T030 — PasswordResetRequestDto.java
  T031 — PasswordResetConfirmDto.java
Then sequentially:
  T032 — PasswordResetService.java
  T033 — AuthController.java (reset endpoints)
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2 Only)

1. Complete **Phase 1**: Setup
2. Complete **Phase 2**: Foundational ← CRITICAL, blocks everything
3. Complete **Phase 3**: US1 Sign-Up → validate independently
4. Complete **Phase 4**: US2 Sign-In → validate independently
5. **STOP and DEMO**: Two P1 stories fully working
6. Continue to US3 and US4 as capacity allows

### Incremental Delivery

| Step | Deliverable | Validation |
|------|-------------|------------|
| Phase 1 + 2 | App boots, DB tables created | Liquibase logs, app startup |
| + Phase 3 | Sign-up works | `POST /v1/api/auth/signup` → 201 |
| + Phase 4 | Sign-in + JWT | `POST /v1/api/auth/signin` → 200 + token |
| + Phase 5 | Password reset | Full reset flow curl test |
| + Phase 6 | Admin APIs | Admin CRUD with role enforcement |
| + Phase 7 | Polish | quickstart.md full pass |

### Parallel Team Strategy

After Phase 2 completes:
- **Developer A**: Phase 3 (US1) → Phase 4 (US2)
- **Developer B**: Phase 5 (US3)
- Both merge, then tackle Phase 6 (US4) together

---

## Task Summary

| Phase | Tasks | User Story | Parallelizable |
|-------|-------|-----------|----------------|
| Phase 1: Setup | T001–T004 | — | T002, T003, T004 |
| Phase 2: Foundational | T005–T018 | — | T006–T016 |
| Phase 3: US1 Sign-Up | T019–T022 | US1 | T019, T020 |
| Phase 4: US2 Sign-In | T023–T029 | US2 | T023, T024 |
| Phase 5: US3 Password Reset | T030–T033 | US3 | T030, T031 |
| Phase 6: US4 Admin APIs | T034–T038 | US4 | T034, T035 |
| Phase 7: Polish | T039–T043 | — | T039, T040, T041 |
| **Total** | **43 tasks** | | |

---

## Notes

- `[P]` tasks operate on different files with no blocking dependencies — safe to run in parallel
- `[US#]` labels provide full traceability from task → user story → spec.md
- No test frameworks, test files, `src/test/` directories, or CI test pipelines — constitution §IV
- All endpoints prefixed `/v1/api/auth/**` or `/v1/api/admin/**`
- Liquibase manages all schema changes — do NOT use `spring.jpa.hibernate.ddl-auto=create`
- JWT library: JJWT 0.12.6 (`io.jsonwebtoken`) — not Spring Security OAuth
- BCrypt via Spring Security's `BCryptPasswordEncoder` — no separate dependency needed
- Commit after each task or logical group; validate at each phase checkpoint before proceeding

