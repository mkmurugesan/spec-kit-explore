--liquibase formatted sql

--changeset usermanagement:000-enable-pgcrypto
CREATE EXTENSION IF NOT EXISTS pgcrypto;
--rollback DROP EXTENSION IF EXISTS pgcrypto;
