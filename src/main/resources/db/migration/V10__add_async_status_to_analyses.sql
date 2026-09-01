-- V10: Add async processing status to analyses

ALTER TABLE analyses ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED';
ALTER TABLE analyses ALTER COLUMN status DROP DEFAULT;

ALTER TABLE analyses ADD COLUMN failure_reason VARCHAR(32);
ALTER TABLE analyses ADD COLUMN started_at TIMESTAMP(6);
ALTER TABLE analyses ADD COLUMN finished_at TIMESTAMP(6);

-- match_score was NOT NULL because a row could only exist after analysis completed.
-- Pending rows have no score yet, so the invariant now lives in the status column.
ALTER TABLE analyses ALTER COLUMN match_score DROP NOT NULL;

CREATE INDEX idx_analysis_status ON analyses (status);