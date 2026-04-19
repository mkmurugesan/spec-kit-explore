# Tasks: Swagger UI / OpenAPI Documentation

**Input**: Design documents from `/specs/003-swagger-openapi-docs/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, contracts/openapi-docs-contract.md ✅

**Tests**: No test tasks — constitution prohibits test artifacts.

**Organization**: Tasks are grouped by user story to enable independent implementation and manual validation of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Exact file paths included in all descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add the single Maven dependency that unlocks all three user stories. Nothing else can be done without this.

- [X] T001 Add `springdoc-openapi-starter-webmvc-ui:2.8.6` dependency to `pom.xml` (inside `<dependencies>`, after the Lombok block)

**Checkpoint**: `mvn dependency:resolve` succeeds — springdoc classes are on the classpath.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Expose springdoc endpoints without authentication. This security change is a prerequisite for all three user stories being independently reachable.

- [X] T002 Update `jwtFilterChain` in `src/main/java/com/example/usermanagement/config/SecurityConfig.java` to add `.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v1/api-docs/**").permitAll()` inside the `authorizeHttpRequests` block (before `.anyRequest().authenticated()`)

**Checkpoint**: Application starts; `curl -o /dev/null -w "%{http_code}" http://localhost:8080/v1/api-docs` returns `200` without an `Authorization` header.

---

## Phase 3: User Story 1 — Browse API Documentation in Browser (Priority: P1) 🎯 MVP

**Goal**: Developer opens a browser, navigates to the Swagger UI URL, and sees all endpoints grouped and described interactively.

**Independent Validation**: Start the application locally → navigate to `http://localhost:8080/swagger-ui/index.html` → confirm all five endpoint groups are visible and expandable.

### Implementation for User Story 1

- [X] T003 [P] [US1] Create `src/main/java/com/example/usermanagement/config/OpenAPIConfig.java` — new `@Configuration` class with a `@Bean` method returning an `OpenAPI` instance that sets `.info(title="User Management API", version="1.0.0", description="REST API for user registration, authentication, password reset, and admin user management.")` and `.components()` with two security schemes: `bearerAuth` (type HTTP, scheme bearer, bearerFormat JWT) and `basicAuth` (type HTTP, scheme basic)
- [X] T004 [P] [US1] Add springdoc properties to `src/main/resources/application.properties`: `springdoc.api-docs.path=/v1/api-docs` and `springdoc.swagger-ui.path=/swagger-ui/index.html`

**Checkpoint**: Navigate to `http://localhost:8080/swagger-ui/index.html` in a browser — fully rendered interactive Swagger UI is displayed with all endpoint groups visible (signup, signin, password-reset/request, password-reset/confirm, admin/**). Lock icon appears on admin endpoints.

---

## Phase 4: User Story 2 — Access OpenAPI JSON/YAML Spec Directly (Priority: P2)

**Goal**: Developer retrieves the raw machine-readable OpenAPI 3.x JSON document for tooling integration (Postman import, SDK generation).

**Independent Validation**: `curl http://localhost:8080/v1/api-docs` returns HTTP 200 with a valid OpenAPI 3.x JSON document listing all endpoints.

### Implementation for User Story 2

> **No additional code changes required.** The dependency (T001), security whitelist (T002), and springdoc properties (T004) together expose `/v1/api-docs` and `/v1/api-docs.yaml` automatically. This phase is a validation milestone only.

**Checkpoint**:
- `curl -s http://localhost:8080/v1/api-docs | python3 -m json.tool` — valid JSON, no parse errors
- `curl -s http://localhost:8080/v1/api-docs | grep -c '"operationId"'` — count ≥ 5 (one per documented operation)
- Import the JSON URL into Postman → all endpoint collections populate correctly

---

## Phase 5: User Story 3 — Unauthenticated Access to Documentation (Priority: P3)

**Goal**: Swagger UI and OpenAPI spec URL are accessible without any JWT token — no 401/403 responses.

**Independent Validation**: Issue `curl -o /dev/null -w "%{http_code}"` requests to both URLs without an `Authorization` header and confirm `200` is returned for each.

### Implementation for User Story 3

> **No additional code changes required.** T002 (security whitelist) already permits both paths without authentication. This phase is a validation milestone only.

**Checkpoint**:
- `curl -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui/index.html` → `200`
- `curl -o /dev/null -w "%{http_code}" http://localhost:8080/v1/api-docs` → `200`
- Neither request includes an `Authorization` header

---

## Final Phase: Polish & Cross-Cutting Concerns

**Purpose**: Confirm the full integration is coherent and no existing behaviour is broken.

- [X] T005 Manual smoke test — start the application with `./mvnw spring-boot:run`, run all five checkpoint curl commands from Phases 2–5, and confirm no regressions on protected endpoints (e.g., `curl http://localhost:8080/v1/api/admin/users` without a token still returns `401`)

---

## Dependency Graph

```
T001 (pom.xml dependency)
  └─► T002 (SecurityConfig whitelist)
        └─► T003 (OpenAPIConfig.java)   [US1 — can run in parallel with T004]
        └─► T004 (application.properties) [US1 — can run in parallel with T003]
              └─► US1 Checkpoint (Swagger UI browsable)
                    └─► US2 Checkpoint (OpenAPI JSON accessible) — no extra code
                          └─► US3 Checkpoint (unauthenticated access) — no extra code
                                └─► T005 (final smoke test)
```

**Parallelisable after T002**: T003 and T004 touch different files and have no mutual dependency — they can be implemented concurrently.

---

## Implementation Strategy

| Order | Task | File | Story |
|-------|------|------|-------|
| 1 | T001 | `pom.xml` | Setup |
| 2 | T002 | `config/SecurityConfig.java` | Foundational |
| 3a | T003 | `config/OpenAPIConfig.java` | US1 |
| 3b | T004 | `application.properties` | US1 |
| 4 | T005 | (manual validation) | Polish |

**MVP Scope**: T001 + T002 + T003 + T004 — all four implementation tasks complete User Story 1 (P1). User Stories 2 and 3 are validation milestones requiring zero additional code.

**Total implementation tasks**: 5 (4 code-change tasks + 1 validation task)  
**Total user stories covered**: 3  
**Parallel opportunities**: 1 (T003 ∥ T004 after T002)






