
ALTER TABLE refresh_token
    ALTER COLUMN family_id TYPE UUID
        USING family_id::uuid;

ALTER TABLE refresh_token
    ADD CONSTRAINT uk_refresh_token_family_id
        UNIQUE (family_id);

ALTER TABLE refresh_token
    ADD COLUMN created_at TIMESTAMP;

ALTER TABLE refresh_token
    ADD COLUMN user_agent VARCHAR(500);

ALTER TABLE refresh_token
    ADD COLUMN ip_address VARCHAR(45);