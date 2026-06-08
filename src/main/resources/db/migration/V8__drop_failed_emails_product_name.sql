-- V8__drop_failed_emails_product_name.sql
-- Elimina columna huérfana product_name de failed_emails.
-- Esta columna fue reemplazada por email_type (añadida en V7).
ALTER TABLE failed_emails DROP COLUMN product_name;
