CREATE TABLE processed_event (
                              id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

                              email VARCHAR(100) NOT NULL,
                              event_type VARCHAR(255) NOT NULL,

                              idem_key VARCHAR(36) UNIQUE NOT NULL,

                              processed_at TIMESTAMPTZ NOT NULL
);