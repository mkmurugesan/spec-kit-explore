# Quickstart: User Management Setup

**Feature**: 001-user-management-setup  
**Date**: 2026-04-19  
**Stack**: Java 21 · Spring Boot 3.4.4 · Maven 3.9.9 · PostgreSQL 16 · Docker Compose

---

## Prerequisites

| Tool | Minimum Version | Check |
|---|---|---|
| Docker Desktop / Docker Engine | 24.x | `docker --version` |
| Docker Compose | v2.x (plugin) | `docker compose version` |
| Java 21 JDK | 21 LTS | `java -version` (only needed for local Maven builds) |
| Maven | 3.9.9 | `mvn -version` (only needed for local Maven builds) |

> **Docker-only path**: If you use the provided `docker-compose.yml` the Spring Boot service is built inside the container — no local JDK or Maven installation is required.

---

## 1. Clone & Navigate

```bash
git clone <repository-url>
cd spec-kit-explore
```

---

## 2. Configuration

All configurable values are set via environment variables in `docker-compose.yml`. The defaults below are safe for local development only — **override before any non-local deployment**.

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db:5432/userdb` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `userapp` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `userapp_pass` | DB password |
| `JWT_ACCESS_TOKEN_EXPIRY` | `15m` | Access token lifetime |
| `JWT_REFRESH_TOKEN_EXPIRY` | `7d` | Refresh token lifetime |
| `AUTH_PASSWORD_RESET_TOKEN_EXPIRY` | `1h` | Password reset token lifetime |
| `JWT_SECRET` | `change-me-in-production-min-256-bit-key` | HMAC-SHA256 signing key |
| `ADMIN_MASTER_USERNAME` | `dev-admin` | Master admin username |
| `ADMIN_MASTER_PASSWORD` | `dev-password` | Master admin password |
| `APP_ENV` | `dev` | Environment mode (`dev` returns reset token in response body) |

---

## 3. Start with Docker Compose

```bash
# From the project root
docker compose up --build
```

This command:
1. Builds the Spring Boot JAR inside a Maven builder container
2. Packages it into a lightweight JRE 21 runtime image
3. Starts the `db` (PostgreSQL 16) service
4. Starts the `app` (Spring Boot) service once the database is healthy
5. Liquibase runs database changelog migrations automatically on first startup

**Expected output (last lines)**:
```
app  | Started UserManagementApplication in X.XXX seconds
app  | Tomcat started on port(s): 8080 (http)
```

---

## 4. Verify the Application is Running

```bash
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
# Expected: { "status": "UP" }
```

---

## 5. API Walkthrough (Manual Validation)

### 5.1 Sign Up

```bash
curl -s -X POST http://localhost:8080/v1/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"SecurePass1","firstName":"Alice","lastName":"Smith"}' \
  | python3 -m json.tool
# Expected: HTTP 201, user summary (no password field)
```

### 5.2 Sign In

```bash
curl -s -X POST http://localhost:8080/v1/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"SecurePass1"}' \
  | python3 -m json.tool
# Expected: HTTP 200, { "accessToken": "...", "refreshToken": "..." }
```

Save the access token for subsequent calls:
```bash
ACCESS_TOKEN=<paste accessToken value here>
```

### 5.3 Password Reset — Request

```bash
curl -s -X POST http://localhost:8080/v1/api/auth/password-reset/request \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com"}' \
  | python3 -m json.tool
# Expected (dev mode): HTTP 200, { "resetToken": "...", "message": "..." }
```

Save the reset token:
```bash
RESET_TOKEN=<paste resetToken value here>
```

### 5.4 Password Reset — Confirm

```bash
curl -s -X POST http://localhost:8080/v1/api/auth/password-reset/confirm \
  -H "Content-Type: application/json" \
  -d "{\"token\":\"$RESET_TOKEN\",\"newPassword\":\"NewSecure99\"}" \
  | python3 -m json.tool
# Expected: HTTP 200, confirmation message
```

### 5.5 Admin — List Users

```bash
curl -s -u dev-admin:dev-password \
  http://localhost:8080/v1/api/admin/users \
  | python3 -m json.tool
# Expected: HTTP 200, array of user objects (no password fields)
```

### 5.6 Admin — Get User by ID

```bash
USER_ID=<paste a user id from the list above>
curl -s -u dev-admin:dev-password \
  http://localhost:8080/v1/api/admin/users/$USER_ID \
  | python3 -m json.tool
# Expected: HTTP 200, user detail object
# Non-existent ID → HTTP 404
```

---

## 6. Stop the Application

```bash
docker compose down
# To also remove the database volume (clean slate):
docker compose down -v
```

---

## 7. Rebuild After Code Changes

```bash
docker compose up --build
```

The `--build` flag forces Docker to re-run the Maven build stage.

---

## 8. Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| `app` exits immediately with `IllegalStateException` | Missing admin credentials | Ensure `ADMIN_MASTER_USERNAME` and `ADMIN_MASTER_PASSWORD` env vars are set |
| `db` connection refused | PostgreSQL not ready | Wait for health check; `docker compose logs db` |
| `401 Unauthorized` on admin endpoints | Wrong credentials | Confirm username/password match env vars |
| Port 8080 already in use | Another process on port 8080 | `lsof -i :8080` and stop the conflicting process |
| Liquibase migration failure | Schema already modified manually | `docker compose down -v` to reset the volume |

