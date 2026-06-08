DROP TABLE IF EXISTS order_event;

CREATE TABLE outbox_event (
                              id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

                              aggregate_type VARCHAR(100) NOT NULL,
                              aggregate_id VARCHAR(255) NOT NULL,
                              event_type VARCHAR(255) NOT NULL,

                              payload TEXT NOT NULL,

                              status VARCHAR(50) NOT NULL
                                    CHECK ( status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED')),

                              retry_count INTEGER NOT NULL DEFAULT 0,

                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

                              error_message TEXT,

                              processed_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_status_created_at
    ON outbox_event(status, created_at);

CREATE INDEX idx_outbox_event_type
    ON outbox_event(event_type);