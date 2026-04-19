# API Contract: Admin Endpoints

**Feature**: 001-user-management-setup  
**Base URL**: `http://localhost:8080` (local) / `https://<host>` (production)  
**API Version prefix**: `/v1`  
**Authentication**: HTTP Basic Auth (master credentials)  
**Date**: 2026-04-19

---

## Authentication

All admin endpoints require HTTP Basic Authentication using the master credentials configured at startup:

| Property | Default (dev only) |
|---|---|
| `admin.master.username` | `dev-admin` |
| `admin.master.password` | `D3v@dm1n!S3cur3` |

**Header format**:
```
Authorization: Basic <base64(username:password)>
```

**curl shorthand**:
```bash
curl -u dev-admin:D3v@dm1n!S3cur3 http://localhost:8080/v1/api/admin/users
```

> **Security note**: These defaults are for local development only. Both values MUST be overridden via environment variables or a secrets manager before staging or production deployment. The application will refuse to start if either value is absent or blank.

---

## GET `/v1/api/admin/users`

List all registered user accounts. Returns non-sensitive fields only; password hashes are never included.

### Request

```http
GET /v1/api/admin/users
Authorization: Basic <credentials>
```

No request body. No query parameters in v1.

### Responses

#### 200 OK — success

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Alice Smith",
    "email": "alice@example.com",
    "status": "ACTIVE",
    "createdAt": "2026-04-19T10:00:00Z"
  },
  {
    "id": "660e8400-e29b-41d4-a716-446655440111",
    "name": "Bob Jones",
    "email": "bob@example.com",
    "status": "ACTIVE",
    "createdAt": "2026-04-19T11:00:00Z"
  }
]
```

Returns an empty array `[]` when no users are registered.

| Field | Type | Description |
|---|---|---|
| `id` | UUID string | User's unique identifier |
| `name` | string | Display name provided at registration |
| `email` | string | Registered email address |
| `status` | string | `ACTIVE` or `INACTIVE` |
| `createdAt` | ISO-8601 UTC | Account creation timestamp |

> **Guarantee**: `passwordHash` is NEVER present in admin list responses (FR-013).

#### 401 Unauthorized — missing or incorrect credentials

```json
{
  "status": 401,
  "error": "Unauthorized",
  "timestamp": "2026-04-19T10:00:00Z"
}
```

Response header includes: `WWW-Authenticate: Basic realm="Admin"`

> Unauthorised access attempts are logged at WARN level (FR-014).

---

## GET `/v1/api/admin/users/{id}`

Retrieve detailed account information for a specific user by their UUID.

### Request

```http
GET /v1/api/admin/users/{id}
Authorization: Basic <credentials>
```

| Path Parameter | Type | Description |
|---|---|---|
| `id` | UUID | The user's unique identifier |

No request body.

### Responses

#### 200 OK — user found

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Alice Smith",
  "email": "alice@example.com",
  "status": "ACTIVE",
  "createdAt": "2026-04-19T10:00:00Z"
}
```

| Field | Type | Description |
|---|---|---|
| `id` | UUID string | User's unique identifier |
| `name` | string | Display name |
| `email` | string | Email address |
| `status` | string | `ACTIVE` or `INACTIVE` |
| `createdAt` | ISO-8601 UTC | Account creation timestamp |

> **Guarantee**: `passwordHash` is NEVER present in the detail response (FR-013).

#### 404 Not Found — user does not exist

```json
{
  "status": 404,
  "error": "User not found",
  "timestamp": "2026-04-19T10:00:00Z"
}
```

#### 401 Unauthorized — missing or incorrect credentials

```json
{
  "status": 401,
  "error": "Unauthorized",
  "timestamp": "2026-04-19T10:00:00Z"
}
```

> Unauthorised access attempts are logged at WARN level (FR-014).

---

## Security Behaviour Summary

| Scenario | Expected HTTP Status | Notes |
|---|---|---|
| Correct master credentials | 200 | Full user data returned |
| Wrong password | 401 | Logged at WARN |
| Missing `Authorization` header | 401 | Logged at WARN |
| Valid user JWT (Bearer token) | 401 | Admin route does not accept user JWTs |
| Non-existent user ID | 404 | Only returned after successful auth |

---

## Error Response Schema (common)

```json
{
  "status": "<HTTP status code>",
  "error": "<human-readable summary>",
  "timestamp": "<ISO-8601 UTC>"
}
```

