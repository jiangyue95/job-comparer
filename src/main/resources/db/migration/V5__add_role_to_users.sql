-- V5: Add role to users
-- Every user has exactly one role. Existing rows are backfilled as USER;
-- the default stays so that an INSERT omitting the role fails closed.

ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
