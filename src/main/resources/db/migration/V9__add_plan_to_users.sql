-- V9: Add plan to users
-- Every user has exactly one subscription plan. Existing rows are backfilled as
-- FREE, which is not a migration artifact but the real state of a non-paying user.
-- Unlike V8's ai_provider, the default is kept: it acts as a fail-safe, so a code
-- path that omits the plan lands on the more restricted tier rather than the
-- unlimited one.

ALTER TABLE users ADD COLUMN plan VARCHAR(20) NOT NULL DEFAULT 'FREE';
