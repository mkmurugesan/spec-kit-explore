--liquibase formatted sql

--changeset usermanagement:001-create-users-table
CREATE TABLE users (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(72)  NOT NULL,
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users (email);
--rollback DROP TABLE users;

