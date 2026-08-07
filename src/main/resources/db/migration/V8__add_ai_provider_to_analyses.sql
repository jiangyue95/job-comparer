-- V8: Add ai_provider to analyses

ALTER TABLE analyses ADD COLUMN ai_provider VARCHAR(32) NOT NULL DEFAULT 'ANTHROPIC';
ALTER TABLE analyses ALTER COLUMN ai_provider DROP DEFAULT;
