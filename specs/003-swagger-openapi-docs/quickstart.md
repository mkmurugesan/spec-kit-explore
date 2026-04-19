# Quickstart: Swagger UI / OpenAPI Documentation

**Feature**: 003-swagger-openapi-docs  
**Date**: 2026-04-19

---

## Prerequisites

- Application running locally (via `docker-compose up` or `mvn spring-boot:run`)
- Application accessible at `http://localhost:8080`

---

## Access Points After Implementation

| What | URL |
|------|-----|
| Swagger UI (browser) | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON spec | `http://localhost:8080/v1/api-docs` |
| OpenAPI YAML spec | `http://localhost:8080/v1/api-docs.yaml` |

---

## Manual Validation Steps

### 1. Verify Swagger UI renders (SC-001, SC-003, FR-001, FR-005)

```bash
curl -o /dev/null -s -w "%{http_code}" http://localhost:8080/swagger-ui/index.html
# Expected: 200
```

Open `http://localhost:8080/swagger-ui/index.html` in a browser. Confirm:
- Page renders without errors
- All five endpoint groups are visible: `signup`, `signin`, `password-reset/request`, `password-reset/confirm`, and the admin endpoints

### 2. Verify OpenAPI JSON is returned unauthenticated (SC-002, SC-004, FR-002, FR-005)

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/v1/api-docs
# Expected: 200

curl -s http://localhost:8080/v1/api-docs | python3 -m json.tool | head -20
# Expected: valid JSON starting with {"openapi":"3.0.1","info":{...}}
```

### 3. Verify all endpoint groups are documented (SC-005, FR-003)

```bash
curl -s http://localhost:8080/v1/api-docs | python3 -c "
import json,sys
paths = json.load(sys.stdin)['paths']
for p in sorted(paths): print(p)
"
# Expected output includes:
# /v1/api/auth/signup
# /v1/api/auth/signin
# /v1/api/auth/password-reset/request
# /v1/api/auth/password-reset/confirm
# /v1/api/admin/... (one or more admin paths)
```

### 4. Verify security schemes are declared (FR-006, FR-007)

```bash
curl -s http://localhost:8080/v1/api-docs | python3 -c "
import json,sys
schemes = json.load(sys.stdin)['components']['securitySchemes']
print(list(schemes.keys()))
"
# Expected: ['bearerAuth', 'basicAuth']
```

### 5. Authenticate in Swagger UI and make a test request (FR-006)

1. Open `http://localhost:8080/swagger-ui/index.html`
2. Click **Authorize**
3. Under **basicAuth**, enter `dev-admin` / `dev-password`
4. Expand any admin endpoint and click **Try it out → Execute**
5. Confirm `200 OK` response is returned

---

## Build & Run Commands

```bash
# Build (no test phase per constitution)
mvn -DskipTests package

# Run locally
mvn spring-boot:run

# Or via Docker Compose
docker-compose up --build
```

