# API Contract: OpenAPI / Swagger Documentation Endpoints

**Feature**: 003-swagger-openapi-docs  
**Date**: 2026-04-19  
**Format**: REST — documentation-serving endpoints added by springdoc-openapi

---

## New Endpoints Introduced by This Feature

### GET /swagger-ui/index.html

| Field | Value |
|-------|-------|
| Method | GET |
| Path | `/swagger-ui/index.html` (also accessible via `/swagger-ui.html` redirect) |
| Auth required | No |
| Description | Renders the interactive Swagger UI page in a browser |
| Success response | `200 OK` — `text/html` — fully rendered Swagger UI |
| Error responses | `404` if springdoc is misconfigured or disabled |

---

### GET /v1/api-docs

| Field | Value |
|-------|-------|
| Method | GET |
| Path | `/v1/api-docs` |
| Auth required | No |
| Description | Returns the machine-readable OpenAPI 3.x specification in JSON format |
| Success response | `200 OK` — `application/json` — valid OpenAPI 3.x document |
| Error responses | `404` if springdoc is misconfigured |

**Sample response structure**:
```json
{
  "openapi": "3.0.1",
  "info": {
    "title": "User Management API",
    "description": "REST API for user registration, authentication, password reset, and admin user management.",
    "version": "1.0.0"
  },
  "components": {
    "securitySchemes": {
      "bearerAuth": {
        "type": "http",
        "scheme": "bearer",
        "bearerFormat": "JWT"
      },
      "basicAuth": {
        "type": "http",
        "scheme": "basic"
      }
    }
  },
  "paths": { "...all existing endpoints..." }
}
```

---

### GET /v1/api-docs.yaml

| Field | Value |
|-------|-------|
| Method | GET |
| Path | `/v1/api-docs.yaml` |
| Auth required | No |
| Description | Returns the OpenAPI 3.x specification in YAML format |
| Success response | `200 OK` — `application/yaml` — valid OpenAPI 3.x YAML document |

---

## Security Whitelist Changes

The following paths are added to `SecurityConfig.jwtFilterChain` `permitAll` matchers:

| Path Pattern | Reason |
|-------------|--------|
| `/swagger-ui/**` | Swagger UI static assets (JS, CSS, HTML sub-resources) |
| `/swagger-ui.html` | Legacy Swagger UI redirect URL |
| `/v1/api-docs/**` | OpenAPI spec JSON and YAML |

---

## Existing Endpoints Documented (no contract changes)

| Method | Path | Auth |
|--------|------|------|
| POST | `/v1/api/auth/signup` | None |
| POST | `/v1/api/auth/signin` | None |
| POST | `/v1/api/password-reset/request` | None |
| POST | `/v1/api/password-reset/confirm` | None |
| GET | `/v1/api/admin/users` | HTTP Basic (ADMIN) |
| GET | `/v1/api/admin/users/{id}` | HTTP Basic (ADMIN) |
| DELETE | `/v1/api/admin/users/{id}` | HTTP Basic (ADMIN) |

All contracts for the above endpoints remain unchanged.

