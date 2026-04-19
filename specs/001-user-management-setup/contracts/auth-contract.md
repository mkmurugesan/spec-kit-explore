# API Contract: Authentication Endpoints

**Feature**: 001-user-management-setup  
**Base URL**: `http://localhost:8080` (local) / `https://<host>` (production)  
**API Version prefix**: `/v1`  
**Content-Type**: `application/json` (request and response unless noted)  
**Date**: 2026-04-19

---

## POST `/v1/api/auth/signup`

Register a new user account.

### Request

```http
POST /v1/api/auth/signup
Content-Type: application/json
```

```json
{
  "name": "Alice Smith",
  "email": "alice@example.com",
  "password": "SecurePass1"
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `name` | string | yes | 1–100 characters |
| `email` | string | yes | Valid RFC-5322 email; must be unique |
| `password` | string | yes | Minimum 8 characters |

### Responses

#### 201 Created — success

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Alice Smith",
  "email": "alice@example.com",
  "status": "ACTIVE",
  "createdAt": "2026-04-19T10:00:00Z"
}
```

#### 400 Bad Request — validation failure

```json
{
  "status": 400,
  "error": "Validation Failed",
  "details": {
    "email": "must be a well-formed email address",
    "password": "size must be between 8 and 2147483647"
  },
  "timestamp": "2026-04-19T10:00:00Z"
}
```

#### 409 Conflict — email already registered

```json
{
  "status": 409,
  "error": "Email already in use",
  "timestamp": "2026-04-19T10:00:00Z"
}
```

---

## POST `/v1/api/auth/signin`

Authenticate a registered user and obtain token pair.

### Request

```http
POST /v1/api/auth/signin
Content-Type: application/json
```

```json
{
  "email": "alice@example.com",
  "password": "SecurePass1"
}
```

| Field | Type | Required |
|---|---|---|
| `email` | string | yes |
| `password` | string | yes |

### Responses

#### 200 OK — success

```json
{
  "accessToken": "<signed JWT>",
  "refreshToken": "<signed JWT>",
  "tokenType": "Bearer",
  "accessTokenExpiresIn": 900,
  "refreshTokenExpiresIn": 604800
}
```

| Field | Description |
|---|---|
| `accessToken` | Short-lived JWT (default 15 min = 900 s) for protected endpoints |
| `refreshToken` | Long-lived JWT (default 7 days = 604800 s) to obtain new access tokens |
| `tokenType` | Always `"Bearer"` |
| `accessTokenExpiresIn` | Seconds until access token expires |
| `refreshTokenExpiresIn` | Seconds until refresh token expires |

#### 401 Unauthorized — invalid credentials (email not found OR wrong password)

```json
{
  "status": 401,
  "error": "Invalid credentials",
  "timestamp": "2026-04-19T10:00:00Z"
}
```

> **Security note**: The response body MUST NOT reveal whether the email exists.

---

## POST `/v1/api/auth/password-reset/request`

Initiate a password reset for a registered email address.

### Request

```http
POST /v1/api/auth/password-reset/request
Content-Type: application/json
```

```json
{
  "email": "alice@example.com"
}
```

| Field | Type | Required |
|---|---|---|
| `email` | string | yes |

### Responses

#### 200 OK — dev mode (APP_ENV=dev)

```json
{
  "message": "Password reset token issued.",
  "resetToken": "a3f9b2c1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1"
}
```

#### 200 OK — production mode (APP_ENV != dev)

```json
{
  "message": "If the email is registered, a reset link has been sent."
}
```

> **Security note**: Both registered and unregistered emails return `200` with the same production message to avoid email enumeration.

#### 400 Bad Request — invalid email format

```json
{
  "status": 400,
  "error": "Validation Failed",
  "details": { "email": "must be a well-formed email address" },
  "timestamp": "2026-04-19T10:00:00Z"
}
```

---

## POST `/v1/api/auth/password-reset/confirm`

Complete password reset using a valid token and set a new password.

### Request

```http
POST /v1/api/auth/password-reset/confirm
Content-Type: application/json
```

```json
{
  "token": "a3f9b2c1d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1",
  "newPassword": "NewSecure99"
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `token` | string | yes | 64-character hex string |
| `newPassword` | string | yes | Minimum 8 characters |

### Responses

#### 200 OK — success

```json
{
  "message": "Password updated successfully."
}
```

#### 400 Bad Request — token invalid, expired, or already used

```json
{
  "status": 400,
  "error": "Invalid or expired reset token",
  "timestamp": "2026-04-19T10:00:00Z"
}
```

#### 400 Bad Request — password too short

```json
{
  "status": 400,
  "error": "Validation Failed",
  "details": { "newPassword": "size must be between 8 and 2147483647" },
  "timestamp": "2026-04-19T10:00:00Z"
}
```

---

## Error Response Schema (common)

All error responses follow this structure:

```json
{
  "status": "<HTTP status code>",
  "error": "<human-readable summary>",
  "details": { "<field>": "<message>" },
  "timestamp": "<ISO-8601 UTC>"
}
```

`details` is omitted for non-validation errors.

