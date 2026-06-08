-- Renombra 'status' a 'outbox_status' para alinear con la entity.
-- Se elimina el índice que referencia 'status' antes de renombrar.
DROP INDEX IF EXISTS idx_outbox_status_created_at;

ALTER TABLE outbox_event RENAME COLUMN status TO outbox_status;

CREATE INDEX idx_outbox_status_created_at
    ON outbox_event(outbox_status, created_at);