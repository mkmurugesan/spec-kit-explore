# Data Model: User Management Setup

**Feature**: 001-user-management-setup  
**Date**: 2026-04-19  
**Database**: PostgreSQL 16  
**ORM**: Spring Data JPA (Hibernate 6)  
**Schema Migration**: Liquibase Core (managed by Spring Boot BOM)

---

## Entities

### 1. User

Represents a registered user account.

#### JPA Entity

```java
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status; // ACTIVE | INACTIVE
}
```

#### Table Schema (Liquibase `db/changelog/changes/001-create-users-table.sql`)

```sql
CREATE TABLE users (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL,
    password_hash VARCHAR(72) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users (email);
```

#### Field Definitions

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK, NOT NULL, auto-generated | Uses PostgreSQL `gen_random_uuid()` |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE, indexed | Lowercased before storage |
| `password_hash` | VARCHAR(72) | NOT NULL | BCrypt hash (60 chars + headroom) |
| `created_at` | TIMESTAMPTZ | NOT NULL, defaulted | Set at insert time, never updated |
| `status` | VARCHAR(20) | NOT NULL, default `ACTIVE` | Enum: `ACTIVE`, `INACTIVE` |

#### Validation Rules

- `email`: must match RFC-5322 format (`@Email` annotation); must be unique (enforced at DB + service layer)
- `password`: minimum 8 characters enforced before hashing; never stored in plaintext

---

### 2. PasswordResetToken

Represents a single-use, time-limited token issued during a password reset request.

#### JPA Entity

```java
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used", nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

#### Table Schema (Liquibase `db/changelog/changes/002-create-password-reset-tokens-table.sql`)

```sql
CREATE TABLE password_reset_tokens (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    token       VARCHAR(64) NOT NULL,
    user_id     UUID        NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_token UNIQUE (token),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_prt_user_id ON password_reset_tokens (user_id);
CREATE INDEX idx_prt_token ON password_reset_tokens (token);
```

#### Field Definitions

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `id` | UUID | PK, NOT NULL, auto-generated | |
| `token` | VARCHAR(64) | NOT NULL, UNIQUE | SecureRandom hex (32 bytes → 64 hex chars) |
| `user_id` | UUID | NOT NULL, FK → users.id | Cascades delete when user is deleted |
| `expires_at` | TIMESTAMPTZ | NOT NULL | `now() + auth.password-reset.token.expiry` (default 1h) |
| `used` | BOOLEAN | NOT NULL, default FALSE | Set to TRUE immediately after successful reset |
| `created_at` | TIMESTAMPTZ | NOT NULL, defaulted | Audit trail |

#### Business Rules

- **Invalidation on new request**: Before inserting a new token, `UPDATE password_reset_tokens SET used = TRUE WHERE user_id = ? AND used = FALSE AND expires_at > NOW()` (FR-010a)
- **Single-use**: After a successful password reset, `used` is set to `TRUE`; any subsequent use returns 400
- **Expiry**: `expires_at` checked at confirm time; expired tokens return 400

---

### 3. AdminCredential (Configuration-level — no database table)

Admin credentials are loaded exclusively from application properties at startup. There is no `admin_credentials` table.

```properties
admin.master.username=dev-admin
admin.master.password=dev-password
```

A `@PostConstruct`-guarded `@ConfigurationProperties` bean validates both values are non-blank at startup (FR-012a). If either is missing, the application throws `IllegalStateException` and refuses to start.

---

## Entity Relationships

```
users (1) ──────────────── (N) password_reset_tokens
       ^                              |
       └──── fk: user_id ────────────┘
```

- One `User` can have multiple `PasswordResetToken` records (history)
- At any point in time, only one token per user should be active (not `used` and not expired)
- Invalidation is enforced at the service layer before each new token insertion

---

## State Transitions

### User.status

```
[registration] → ACTIVE
ACTIVE → INACTIVE  (future: account suspension — post-v1)
```

Only `ACTIVE` users may sign in or request password resets.

### PasswordResetToken lifecycle

```
[request received] → created (used=false, expires_at=now+1h)
[new request for same user] → previous tokens: used=true (invalidated)
[confirm with valid token] → used=true
[expires_at passes] → logically expired (used check + expires_at check)
```

---

## Indexes Summary

| Table | Index | Columns | Purpose |
|---|---|---|---|
| `users` | `uq_users_email` | `email` | Uniqueness + lookup by email |
| `users` | `idx_users_email` | `email` | Fast sign-in lookup |
| `password_reset_tokens` | `uq_password_reset_token` | `token` | Uniqueness + fast token lookup |
| `password_reset_tokens` | `idx_prt_user_id` | `user_id` | Fast invalidation query by user |
| `password_reset_tokens` | `idx_prt_token` | `token` | Fast confirm lookup |

