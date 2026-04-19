# Feature Specification: User Management Setup

**Feature Branch**: `001-user-management-setup`  
**Created**: 2026-04-19  
**Status**: Clarified  
**Input**: User description: "User Management Setup - Develop API's that allow user's to perform signup, signin feature. Users should be able to change their password if they forget. Lets use all the relevant security aspect to secure the user's information since its sensitive. There should be also admin API's to check user's data for debugging and should be only allowed for developers using custom auth master username and password (create this and document it)."

## User Scenarios & Validation *(mandatory)*

### User Story 1 - New User Registration / Sign-Up (Priority: P1)

A new user provides their name, email address, and a chosen password to create an account. The system validates the input, ensures the email is not already registered, securely stores the credentials, and confirms the account creation.

**Why this priority**: Sign-up is the foundation of every other user interaction. Without it, no user can access the system. It must be available before all other flows.

**Independent Validation**: Call the sign-up endpoint with valid credentials and confirm a success response is returned. Call it again with the same email and confirm a conflict/duplicate response is returned. Delivers a working registration flow that can be demonstrated end-to-end.

**Acceptance Scenarios**:

1. **Given** the registration endpoint is available, **When** a user submits a valid unique email and a password of at least 8 characters, **Then** the system creates the account and returns a success confirmation.
2. **Given** an email address is already registered, **When** a user attempts to sign up with that same email, **Then** the system rejects the request and returns a clear error indicating the email is already in use.
3. **Given** a user submits a password shorter than 8 characters, **When** the registration request is received, **Then** the system rejects it with a descriptive validation error.
4. **Given** a user submits a malformed email address, **When** the registration request is received, **Then** the system rejects it with a descriptive validation error.

---

### User Story 2 - Registered User Sign-In (Priority: P1)

A registered user provides their email and password to authenticate. The system verifies the credentials and, on success, returns a short-lived access token (15 minutes) and a refresh token (7 days) that the user includes in subsequent requests to protected resources.

**Why this priority**: Sign-in is equally critical — without authentication, no protected endpoint can be used. Ranked P1 alongside sign-up because both must exist before any protected API can be accessed.

**Independent Validation**: Call the sign-in endpoint with correct credentials and confirm both an access token and refresh token are returned. Call it with wrong credentials and confirm an unauthorised response is returned. Validates the full authentication loop.

**Acceptance Scenarios**:

1. **Given** a registered user with valid credentials, **When** they call the sign-in endpoint with correct email and password, **Then** the system returns a short-lived access token (default: 15 minutes) and a refresh token (default: 7 days).
2. **Given** a registered user, **When** they call the sign-in endpoint with an incorrect password, **Then** the system returns an unauthorised response and does not reveal whether the email exists.
3. **Given** an email address that is not registered, **When** someone calls the sign-in endpoint, **Then** the system returns an unauthorised response without leaking account existence.
4. **Given** a valid, non-expired access token, **When** the user accesses a protected endpoint with that token, **Then** the system grants access.
5. **Given** an expired or invalid access token, **When** the user accesses a protected endpoint, **Then** the system returns an unauthorised response.

---

### User Story 3 - Forgotten Password Reset (Priority: P2)

A user who cannot remember their password requests a password reset. The system generates a time-limited single-use reset token. In development mode, the token is returned directly in the API response. The user presents the token with a new password to regain access.

**Why this priority**: Password recovery is a core security feature that prevents permanent account lockout. It is less critical than registration and sign-in but necessary for a complete and production-ready user management flow.

**Independent Validation**: Trigger a password reset request for a registered email, capture the reset token from the API response (dev mode), use it to set a new password, and confirm the user can sign in with the new password. Verify that requesting a second token invalidates the first.

**Acceptance Scenarios**:

1. **Given** a registered user, **When** they request a password reset with their email address, **Then** the system issues a time-limited reset token; in development mode the token is included in the response body; in production mode only a generic confirmation message is returned.
2. **Given** a valid, unexpired reset token, **When** the user submits it with a new password of at least 8 characters, **Then** the system updates the password and invalidates the token.
3. **Given** an expired or already-used reset token, **When** the user attempts to reset their password, **Then** the system rejects the request with a clear error.
4. **Given** an email address that is not registered, **When** a reset is requested, **Then** the system returns a generic confirmation (does not reveal whether the email exists).
5. **Given** a user who already has an unexpired reset token, **When** they request a new reset token, **Then** the system invalidates all previous unexpired tokens for that user and issues a new one.

---

### User Story 4 - Developer Admin: View User Data (Priority: P3)

A developer uses a dedicated admin set of endpoints to inspect user account data for debugging purposes. Access to these endpoints requires a master username and password defined at system configuration time.

**Why this priority**: Admin/debug APIs are supporting infrastructure for developers, not end-user features. They are valuable but do not block the core user authentication flows.

**Independent Validation**: Call the admin endpoints with the correct master credentials and confirm user data is returned. Call them with missing or incorrect credentials and confirm access is denied.

**Acceptance Scenarios**:

1. **Given** the correct master username and password are supplied via HTTP Basic Auth, **When** a developer calls the admin user-list endpoint, **Then** the system returns a list of registered users with non-sensitive debug information (no plaintext passwords).
2. **Given** the correct master credentials, **When** a developer calls the admin user-detail endpoint with a specific user identifier, **Then** the system returns that user's account details.
3. **Given** incorrect or missing master credentials, **When** any admin endpoint is called, **Then** the system returns an unauthorised response and logs the access attempt.
4. **Given** a non-admin token (regular user JWT), **When** the admin endpoints are called, **Then** the system rejects the request.

---

### Edge Cases

- **Race condition on duplicate sign-up**: The database MUST enforce a unique constraint on the email column; the last concurrent insert will receive a conflict error (409). No application-level locking is required.
- **Brute-force / repeated failed sign-in**: No lockout or rate limiting in v1. The system returns a generic 401 on every failure without leaking account existence. Lockout is a post-v1 enhancement.
- **Multiple password reset token requests**: Each new reset request invalidates all previous unexpired tokens for that user. Only one live token exists at any time.
- **Admin credentials not configured at startup**: The application MUST fail to start (startup validation) if `admin.master.username` or `admin.master.password` is missing or empty.
- **Admin user-detail with non-existent ID**: The system returns 404 with a generic "user not found" message.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow a new user to register with a unique email address and a password of at least 8 characters.
- **FR-002**: System MUST reject registration attempts where the email address is already associated with an existing account.
- **FR-003**: System MUST validate email format and minimum password length (8 characters) during registration and return descriptive errors on failure.
- **FR-004**: System MUST securely store user passwords using an industry-standard one-way hashing algorithm (e.g., bcrypt); plaintext passwords MUST never be stored or logged.
- **FR-005**: System MUST authenticate registered users by verifying their email and password and, on success, return a signed short-lived access token and a refresh token.
- **FR-006**: System MUST reject sign-in attempts with invalid credentials without revealing whether the email address is registered.
- **FR-007**: System MUST protect sensitive endpoints so that only requests carrying a valid, non-expired access token are permitted.
- **FR-008**: System MUST allow a registered user to initiate a password reset by providing their email address.
- **FR-009**: System MUST generate a time-limited, single-use password reset token. In development mode, the token MUST be returned in the API response body. In production mode, only a generic confirmation is returned (email delivery is post-v1).
- **FR-010**: System MUST allow the user to set a new password of at least 8 characters by presenting a valid reset token; the token MUST be invalidated immediately after use or on expiry.
- **FR-010a**: When a new password reset token is requested, the system MUST invalidate all previous unexpired reset tokens for that user before issuing a new one.
- **FR-011**: System MUST expose admin-only endpoints that allow developers to list and inspect user account records for debugging.
- **FR-012**: Admin endpoints MUST be protected by a dedicated master username and password configured at application startup (HTTP Basic Auth); these credentials MUST be documented and MUST NOT be the same as any user account.
- **FR-012a**: The application MUST fail to start if the master admin username or password is not configured.
- **FR-013**: Admin endpoints MUST never return plaintext passwords or raw password hashes in their responses.
- **FR-014**: System MUST log unauthorised access attempts to admin endpoints.
- **FR-015**: System MUST apply appropriate input sanitisation on all endpoints to prevent injection-style attacks.
- **FR-016**: All communication with the API MUST be conducted over HTTPS in production deployments.
- **FR-017**: Access token expiry (default: 15 minutes) and refresh token expiry (default: 7 days) MUST be configurable via application properties without a code change.
- **FR-018**: The admin user-detail endpoint MUST return 404 with a generic "user not found" message when the requested user identifier does not exist.

### Token Expiry Defaults (Configurable)

| Property | Default Value | Notes |
|----------|--------------|-------|
| `jwt.access-token.expiry` | `15m` | Short-lived; rotate frequently |
| `jwt.refresh-token.expiry` | `7d` | Longer-lived; used to obtain new access tokens |
| `auth.password-reset.token.expiry` | `1h` | Single-use; invalidated on first use or expiry |

### Admin Credentials (Documented)

The master admin credentials are fixed configuration values loaded from application properties at startup:

| Property | Default Value (Development Only) | Notes |
|----------|----------------------------------|-------|
| `admin.master.username` | `dev-admin` | Change before any non-local deployment |
| `admin.master.password` | `dev-password` | Rotate before any non-local deployment |

> **Security Note**: These defaults are for local development only. Both values MUST be overridden via environment variables or a secrets manager before staging or production deployment. The application will refuse to start if either value is absent.

### Key Entities

- **User**: Represents a registered account. Key attributes: unique identifier, email address (unique, indexed), hashed password, account creation timestamp, account status (active/inactive).
- **PasswordResetToken**: Represents a single-use, time-limited credential for password reset. Key attributes: token value, associated user identifier, expiry timestamp, used flag. Previous tokens for the same user are invalidated when a new one is issued.
- **AdminCredential**: Configuration-level entity (not stored in the user database) representing the master username and password loaded from application properties at startup.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new user can complete registration, sign in, and access a protected resource in a single uninterrupted flow, verified manually via an HTTP client (curl/Postman) in under 3 minutes of elapsed wall-clock time.
- **SC-002**: All authentication endpoints return appropriate HTTP status codes (201 for registration, 200 for sign-in with token pair, 401 for invalid credentials, 400 for validation errors) when called manually and observed in response logs.
- **SC-003**: Passwords are provably never stored or transmitted in plaintext — confirmed by inspecting the database record directly after registration and verifying only a hash is present.
- **SC-004**: A password reset flow completes end-to-end (request → token in response → new password → sign-in) and is validated manually in a development environment.
- **SC-004a**: Requesting a second reset token while the first is still valid results in the first token being rejected — confirmed manually by attempting to use the original token after a new one is issued.
- **SC-005**: Admin endpoints return user data only when correct master credentials are supplied, and return 401 for all other access attempts — verified manually with at least three negative test cases (wrong password, missing header, user token).
- **SC-006**: Admin response payloads, inspected manually, contain no plaintext passwords or raw hashes.
- **SC-007**: All endpoints respond within an acceptable time under normal load, observed manually during local application runs with no visible delay above 1 second for individual requests.
- **SC-008**: The application fails to start when admin credentials are missing from configuration — verified by removing the properties and observing startup failure.

## API Behavior *(include if feature exposes REST endpoints)*

| Method | Path | Description | Success Response |
|--------|------|-------------|-----------------|
| POST | `/v1/api/auth/signup` | Register a new user account | 201 + user summary (no password) |
| POST | `/v1/api/auth/signin` | Authenticate and receive token pair | 200 + access token + refresh token |
| POST | `/v1/api/auth/password-reset/request` | Initiate a password reset | 200 + token in body (dev) / generic message (prod) |
| POST | `/v1/api/auth/password-reset/confirm` | Complete reset with token + new password | 200 + confirmation |
| GET | `/v1/api/admin/users` | List all registered users (admin only) | 200 + user list |
| GET | `/v1/api/admin/users/{id}` | Get details for a specific user (admin only) | 200 + user detail / 404 if not found |

**Manual Validation**: All endpoints MUST be validated via curl, Postman, or equivalent HTTP client; call logs and response payloads MUST be captured in feature docs.

## Assumptions

- All users are human end-users accessing the API; no machine-to-machine (service account) registration is required for v1.
- Email delivery for password reset tokens is out of scope for v1; reset tokens are returned directly in the API response body in development mode only.
- The API is consumed by client applications (web or mobile front-end) that are out of scope; only the backend endpoints are in scope.
- A persistent relational database is available and configured in the Spring Boot application; schema management uses Liquibase (decided in plan phase).
- HTTPS enforcement is assumed to be handled at the infrastructure/proxy layer in production; the application may operate over HTTP locally.
- Password minimum length is 8 characters; no additional complexity rules (uppercase, numbers, special characters) are enforced in v1.
- Rate limiting and account lockout after repeated failed sign-in attempts are post-v1 enhancements; the system returns a generic 401 on every failure without lockout.
- The master admin credentials (username and password) are provided through application configuration properties; the application fails to start if they are not set.
- Token-based authentication uses JWT (JSON Web Tokens) with a 15-minute access token and 7-day refresh token as defaults; both durations are configurable via application properties.
- No role-based access control (RBAC) beyond the binary "regular user vs. admin" distinction is required for v1.
- A new password reset request always invalidates all previous unexpired reset tokens for the same user; only one live reset token exists per user at any time.
