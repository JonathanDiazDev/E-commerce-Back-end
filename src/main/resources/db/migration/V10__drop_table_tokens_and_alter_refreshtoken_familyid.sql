DROP TABLE IF EXISTS tokens;

ALTER TABLE refresh_token DROP CONSTRAINT uk_refresh_token_family_id;