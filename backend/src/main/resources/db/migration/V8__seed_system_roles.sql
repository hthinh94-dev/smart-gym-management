-- V8__seed_system_roles.sql
-- Seed static system roles

INSERT INTO roles (name, created_at, updated_at)
VALUES
    ('ROLE_ADMIN',  CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ROLE_MEMBER', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('ROLE_PT',     CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
