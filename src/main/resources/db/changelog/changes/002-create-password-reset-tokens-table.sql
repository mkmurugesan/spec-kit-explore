--liquibase formatted sql

--changeset usermanagement:002-create-password-reset-tokens-table
CREATE TABLE password_reset_tokens (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    token      VARCHAR(64) NOT NULL,
    user_id    UUID        NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_token UNIQUE (token),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_prt_user_id ON password_reset_tokens (user_id);
CREATE INDEX idx_prt_token ON password_reset_tokens (token);
--rollback DROP TABLE password_reset_tokens;

