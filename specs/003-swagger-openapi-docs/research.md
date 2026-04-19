# Research: Swagger UI / OpenAPI Documentation

**Feature**: 003-swagger-openapi-docs  
**Date**: 2026-04-19  
**Status**: Complete — no unresolved NEEDS CLARIFICATION items

---

## Decision 1 — Library choice: springdoc-openapi vs springfox

**Decision**: Use `springdoc-openapi-starter-webmvc-ui` (v2.x)  
**Rationale**: springdoc-openapi has native Spring Boot 3 and Java 21 support, active maintenance, and auto-discovers Spring MVC controllers without additional annotations. springfox is effectively abandoned (last release 2020), incompatible with Spring Boot 3, and would require extensive workarounds.  
**Alternatives considered**: springfox 3.x — rejected because it is not compatible with Spring Boot 3.x and is no longer maintained.

**Confirmed version**: `2.8.6` (latest stable as of 2026-04-19; managed by Spring Boot parent BOM for minor version alignment).  
**Dependency to add**:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.6</version>
</dependency>
```
Justification (Constitution III): No standard-library or existing-project alternative exists for generating an interactive OpenAPI UI. The single starter bundles both `springdoc-openapi-starter-webmvc-api` and the Swagger UI static assets; splitting them would not reduce the transitive footprint.

---

## Decision 2 — API docs path: custom `/v1/api-docs` vs default `/v3/api-docs`

**Decision**: Expose docs at `/v1/api-docs` (custom, per user request) while keeping Swagger UI at `/swagger-ui/index.html`.  
**Rationale**: Aligns with the project's existing `/v1/` path prefix convention. The path is configurable via `springdoc.api-docs.path` without any code change.  
**Alternatives considered**: Default `/v3/api-docs` — rejected because the user explicitly requested `/v1/api-docs`.

---

## Decision 3 — Security scheme exposure in OpenAPI bean

**Decision**: Register two security schemes in the `OpenAPI` bean:
1. `bearerAuth` — HTTP Bearer (JWT), applied globally to indicate protected routes.
2. `basicAuth` — HTTP Basic, for the admin chain.

Both schemes are declared at the top-level components; individual operations are annotated with `@SecurityRequirement` where needed.  
**Rationale**: Swagger UI's "Authorize" button is only populated when security schemes are declared in the OpenAPI bean. Without them, testers cannot authenticate in-browser.  
**Alternatives considered**: Relying solely on springdoc auto-detection — rejected because springdoc does not automatically infer custom `JwtAuthFilter` security requirements; an explicit `OpenAPI` bean is required.

---

## Decision 4 — SecurityConfig whitelist paths

**Decision**: Add the following `permitAll` matchers to the `jwtFilterChain` (Order 2):
- `/swagger-ui/**`
- `/v1/api-docs/**`
- `/swagger-ui.html`

**Rationale**: springdoc serves the Swagger UI under `/swagger-ui/` (multiple JS/CSS sub-resources) and the spec under the configured `api-docs.path`. All must be permitted to satisfy FR-005 / SC-003 / SC-004.  
**Alternatives considered**: A separate `@Order(0)` security filter chain for docs paths — rejected as unnecessary complexity; a `permitAll` matcher in the existing chain is sufficient and simpler.

---

## Decision 5 — OpenAPIConfig class placement

**Decision**: Add a single `OpenAPIConfig.java` in the existing `com.example.usermanagement.config` package.  
**Rationale**: Keeps all Spring configuration in one place consistent with the current project structure. No new package is needed.  
**Alternatives considered**: Inline `@Bean` in the main application class — rejected because it mixes application bootstrap with API metadata configuration, violating Principle I (single responsibility).

---

## No NEEDS CLARIFICATION items remain.

All technical decisions above are fully resolved and feed directly into the implementation plan and contracts.

