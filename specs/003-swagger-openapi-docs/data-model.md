# Data Model: Swagger UI / OpenAPI Documentation

**Feature**: 003-swagger-openapi-docs  
**Date**: 2026-04-19

---

## Overview

This feature introduces **zero new entities, zero new database tables, and zero new DTOs**.  
It is a documentation-only addition that reflects the already-existing domain model.

---

## Existing Entities Exposed via OpenAPI (read-only reference)

The following entities/DTOs are already defined in the codebase and will be surfaced automatically by springdoc through classpath scanning of `@RestController` classes and their `@RequestBody` / response types.

| Class | Role | Package |
|-------|------|---------|
| `SignupRequest` | Request body for `POST /v1/api/auth/signup` | `dto` |
| `SignupResponse` | Response for signup | `dto` |
| `SigninRequest` | Request body for `POST /v1/api/auth/signin` | `dto` |
| `SigninResponse` | Response for signin (includes tokens) | `dto` |
| `PasswordResetRequestDto` | Request body for `POST /v1/api/auth/password-reset/request` | `dto` |
| `PasswordResetConfirmDto` | Request body for `POST /v1/api/auth/password-reset/confirm` | `dto` |
| `UserSummaryDto` | Response element for admin user listing | `dto` |

No field modifications, validation changes, or new classes are required by this feature.

---

## New Configuration Artifact

| Class | Type | Purpose |
|-------|------|---------|
| `OpenAPIConfig` | `@Configuration` Spring bean | Declares API metadata (title, version, description) and security schemes (Bearer JWT, HTTP Basic) |

### `OpenAPIConfig` — Logical Structure

```
OpenAPIConfig
├── openAPI() : OpenAPI
│   ├── info()
│   │   ├── title      → "User Management API"
│   │   ├── version    → "1.0.0"
│   │   └── description → "REST API for user registration, authentication, password reset, and admin user management."
│   └── components()
│       └── securitySchemes
│           ├── bearerAuth → type: HTTP, scheme: bearer, bearerFormat: JWT
│           └── basicAuth  → type: HTTP, scheme: basic
```

No state, no persistence, no validation rules.

---

## State Transitions

Not applicable — this feature has no stateful entities.

