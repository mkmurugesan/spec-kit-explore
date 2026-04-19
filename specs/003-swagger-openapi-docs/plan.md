# Implementation Plan: Swagger UI / OpenAPI Documentation

**Branch**: `003-swagger-openapi-docs` | **Date**: 2026-04-19 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/003-swagger-openapi-docs/spec.md`

## Summary

Add interactive Swagger UI and machine-readable OpenAPI 3.x documentation to the existing Spring Boot User Management application by integrating the `springdoc-openapi-starter-webmvc-ui` library (v2.8.6). The implementation is documentation-only: one new Maven dependency, one new `@Configuration` class (`OpenAPIConfig`), two security whitelist entries in `SecurityConfig`, and two `springdoc.*` properties in `application.properties`. No business logic, no persistence, no test artifacts.

## Technical Context

**Language/Version**: Java 21 LTS (confirmed in `pom.xml`)  
**Framework**: Spring Boot 3.4.4 (confirmed in `pom.xml`)  
**Build Tool**: Maven (managed via Spring Boot parent BOM)  
**Primary Dependencies added**: `springdoc-openapi-starter-webmvc-ui:2.8.6` — sole addition; justified because no standard-library alternative exists for generating an interactive OpenAPI UI (Constitution III)  
**Storage**: PostgreSQL (existing; not touched by this feature)  
**Testing**: Prohibited by constitution — manual validation only  
**Target Platform**: JVM backend (Linux/Docker)  
**Performance Goals**: OpenAPI spec JSON returned in < 1 s on local machine (SC-002)  
**Constraints**: Zero new business logic; zero test artifacts; zero frontend changes  
**Scale/Scope**: Documentation-only change; runtime overhead is negligible (static JSON generation at first request, then cached by springdoc)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

- [x] **Latest Spring Boot**: `3.4.4` is the latest stable release pinned in `pom.xml`; no version change required.
- [x] **Latest Maven**: Managed via Spring Boot parent BOM; no separate Maven version pinning needed.
- [x] **Layered architecture**: Feature adds only a `config/` class (`OpenAPIConfig`). No controller, service, or repository changes; existing layer boundaries are preserved.
- [x] **Minimal dependencies**: One dependency added (`springdoc-openapi-starter-webmvc-ui`). No standard-library or existing-project alternative exists for Swagger UI generation. Documented in `research.md` Decision 1.
- [x] **No test frameworks**: Dependency list contains NO JUnit, Mockito, Testcontainers, or any test library.
- [x] **No test artifacts**: No `src/test/` directories or test classes are planned.
- [x] **Backend only**: No frontend framework referenced. Swagger UI assets are served by springdoc as static resources inside the JAR; no npm, no build tool, no UI framework is added.

**GATE RESULT: PASS** — All checks satisfied; no violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/003-swagger-openapi-docs/
├── plan.md              ← this file
├── research.md          ← Phase 0 output
├── data-model.md        ← Phase 1 output
├── quickstart.md        ← Phase 1 output
├── contracts/
│   └── openapi-docs-contract.md  ← Phase 1 output
└── tasks.md             ← Phase 2 output (/speckit.tasks — NOT created by /speckit.plan)
```

### Source Code Changes (repository root)

```text
pom.xml                                        ← ADD springdoc-openapi-starter-webmvc-ui dependency
src/
└── main/
    ├── java/
    │   └── com/example/usermanagement/
    │       └── config/
    │           ├── OpenAPIConfig.java          ← NEW — OpenAPI bean with metadata + security schemes
    │           └── SecurityConfig.java         ← MODIFY — add /swagger-ui/** and /v1/api-docs/** to permitAll
    └── resources/
        └── application.properties              ← MODIFY — add springdoc properties
```

## Implementation Detail

### 1. `pom.xml` — Add dependency

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.6</version>
</dependency>
```

Place after the existing Lombok dependency block.

---

### 2. `OpenAPIConfig.java` — New configuration class

**Package**: `com.example.usermanagement.config`  
**Annotations**: `@Configuration`

```java
package com.example.usermanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI userManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Management API")
                        .version("1.0.0")
                        .description("REST API for user registration, authentication, " +
                                     "password reset, and admin user management."))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")));
    }
}
```

---

### 3. `SecurityConfig.java` — Whitelist Swagger paths

In `jwtFilterChain` (Order 2), updated `authorizeHttpRequests` block:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/", "/index.html", "/styles.css", "/app.js").permitAll()
    .requestMatchers("/v1/api/auth/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v1/api-docs/**").permitAll()
    .anyRequest().authenticated()
)
```

---

### 4. `application.properties` — springdoc configuration

```properties
# OpenAPI / Swagger
springdoc.api-docs.path=/v1/api-docs
springdoc.swagger-ui.path=/swagger-ui/index.html
```

---

## Complexity Tracking

No constitution violations. This section is intentionally blank.
