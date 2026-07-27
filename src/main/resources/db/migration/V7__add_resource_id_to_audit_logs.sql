-- V7: Add resource_id to audit_logs
--
-- Extends the table beyond the security-only scope declared in V6, so it
-- now also records business resource changes (CV, Job, Analysis).
--
-- The resource type is not stored: it is already encoded in the action
-- name (e.g. CV_CREATE), and a separate column would be derivable data
-- that can drift out of sync.
--
-- Nullable: authentication events have no target resource.
-- No foreign key, consistent with user_id: an entry must survive deletion
-- of the row it refers to.

ALTER TABLE audit_logs ADD COLUMN resource_id BIGINT;

COMMENT ON TABLE audit_logs IS
    'Append-only record of security events and business resource changes. Rows are never updated or deleted. No foreign keys: an entry must survive deletion of the user or the resource it refers to.';
