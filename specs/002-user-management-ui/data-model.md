# Data Model: User Management UI

**Phase**: 1  
**Branch**: `002-user-management-ui`  
**Date**: 2026-04-19

---

## Overview

This feature has no persistent data model of its own; it is a pure static UI layer that consumes the existing backend API. The "entities" here are **in-browser runtime state objects** that the JavaScript layer manages within the lifecycle of a single page session.

---

## UI State Entities

### 1. `FormState`

Tracks the runtime state of each form independently.

| Field | Type | Description |
|---|---|---|
| `loading` | `boolean` | `true` while a fetch request is in flight; `false` otherwise |
| `fieldErrors` | `object` | Map of field name → error message string, populated from API `details` map |
| `globalError` | `string \| null` | Generic error message for non-field errors (4xx domain errors, 5xx, network failures) |
| `values` | `object` | Map of field name → current input value (preserved on error per FR-UI-008) |

**State transitions**:
```
IDLE → LOADING (on submit)
LOADING → IDLE + errors (on API error or network failure)
LOADING → NAVIGATED (on API success)
```

---

### 2. `ApiError`

Derived from the API response body. Parsed by `parseApiError(response, body)` in `app.js`.

| Field | Type | Source | Description |
|---|---|---|---|
| `fieldErrors` | `object \| null` | `body.details` | Field-level validation errors; null if not present |
| `globalMessage` | `string` | `body.error`, `body.message`, or fallback | Human-readable summary for the global error banner |

**Parsing rules** (in priority order):
1. If `response.ok` → not an error; return null.
2. Try `response.json()`. If parse fails → `{ fieldErrors: null, globalMessage: "Something went wrong. Please try again later." }`.
3. If `body.details` is a non-empty object → `fieldErrors = body.details`.
4. `globalMessage = body.error ?? body.message ?? "Something went wrong. Please try again later."`.

**Network failure** (fetch throws, no response):
- `{ fieldErrors: null, globalMessage: "Could not reach the server. Please check your connection." }`.

---

### 3. `SessionTokenPair`

Stored in `sessionStorage` after successful sign-in.

| Key (`sessionStorage`) | Source field | Description |
|---|---|---|
| `um_access_token` | `response.accessToken` | Short-lived JWT for protected endpoints |
| `um_refresh_token` | `response.refreshToken` | Long-lived JWT for token refresh |

**Lifecycle**:
- Written: on successful `POST /v1/api/auth/signin` response.
- Cleared: automatically by browser on tab close (`sessionStorage` semantics). No explicit sign-out UI is in scope for v1.

---

## View / Section Map

Each HTML `<section>` corresponds to a named view. Only one section is visible at a time.

| Section ID | View | Triggered by |
|---|---|---|
| `view-signin` | Sign-In form | Default (page load), after sign-up success, after password reset success |
| `view-signup` | Sign-Up form | "Create account" link on sign-in view |
| `view-forgot-step1` | Forgot Password – email entry | "Forgot password?" link on sign-in view |
| `view-forgot-step2` | Forgot Password – token + new password | After step-1 API success |

---

## API Payloads (consumed, not owned)

Documented in full in `specs/001-user-management-setup/contracts/auth-contract.md`. Summary:

### Sign-Up Request (`POST /v1/api/auth/signup`)
```json
{ "firstName": "string", "lastName": "string", "email": "string", "password": "string" }
```

### Sign-In Request (`POST /v1/api/auth/signin`)
```json
{ "email": "string", "password": "string" }
```

### Password Reset – Step 1 (`POST /v1/api/auth/password-reset/request`)
```json
{ "email": "string" }
```

### Password Reset – Step 2 (`POST /v1/api/auth/password-reset/confirm`)
```json
{ "token": "string", "newPassword": "string" }
```

---

## Validation Rules (client-side)

| Form | Field | Client-side rule |
|---|---|---|
| Sign-Up | `firstName` | Required (non-empty) |
| Sign-Up | `lastName` | Required (non-empty) |
| Sign-Up | `email` | Required (non-empty) |
| Sign-Up | `password` | Required (non-empty) |
| Sign-In | `email` | Required (non-empty) |
| Sign-In | `password` | Required (non-empty) |
| Forgot – Step 1 | `email` | Required (non-empty) |
| Forgot – Step 2 | `token` | Required (non-empty) |
| Forgot – Step 2 | `newPassword` | Required (non-empty) |

> Email format and password length constraints are validated server-side only; the UI surfaces the API error messages verbatim.

