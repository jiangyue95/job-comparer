-- V2: Add username to users
-- username is required (NOT NULL) and globally unique
-- Exising rows are backfilled with a unique placeholder derived from the primary key.

-- 1. Add the column as nullable first, so existing rows don't violate NOT NULL
ALTER TABLE users ADD COLUMN username VARCHAR(50);

-- 2. Backfill existing rows with a guaranteed-unique value based on the primary key
UPDATE users SET username = 'user_' || id WHERE username IS NULL;

-- 3. Now that every row has a value, enforce NOT NULL
ALTER TABLE users ALTER COLUMN username SET NOT NULL;

-- 4. Enforce global uniqueness
ALTER TABLE users ADD CONSTRAINT users_username_key UNIQUE (username);