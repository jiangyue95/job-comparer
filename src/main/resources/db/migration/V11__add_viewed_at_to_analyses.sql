-- V11: Add viewed_at to analyses

ALTER TABLE analyses
    ADD COLUMN viewed_at TIMESTAMP(6);

-- Existing analyses were produced synchronously: the user saw the result at the
-- moment of submission. Leaving them null would surface a backlog of unread
-- items that never existed.
UPDATE analyses
SET viewed_at = analyses.created_at;