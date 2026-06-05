-- ============================================
-- Manual migrations
--
-- PostgreSQL-specific schema changes that JPA cannot express.
-- These must be applied manually after Hibernate creates the schema.
--
-- TODO: replace with Flyway for automated, versioned migrations.
-- ============================================

-- Partial unique index: same cv_name per user, only when active.
-- Allows reusing a name after soft-deleting the previous CV.
CREATE UNIQUE INDEX IF NOT EXISTS uq_cv_user_name_active
    ON cv (user_id, cv_name)
    WHERE deleted_at IS NULL;