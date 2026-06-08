-- V7__fix_failed_emails_email_type.sql
-- V7__fix_failed_emails_email_type.sql
ALTER TABLE failed_emails ADD COLUMN IF NOT EXISTS email_type VARCHAR(255) DEFAULT 'UNKNOWN';
UPDATE failed_emails SET email_type = 'UNKNOWN' WHERE email_type IS NULL;
ALTER TABLE failed_emails ALTER COLUMN email_type SET NOT NULL;