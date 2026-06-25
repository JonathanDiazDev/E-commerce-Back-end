-- =============================================================
-- V11__fix_inventory_movement_check_constraint.sql
-- Corrige el CHECK constraint de movement_type para que coincida
-- con los valores del enum MovementType (RESTOCK, SALE, RETURN, ADJUSTMENT)
-- en lugar de los valores incorrectos anteriores (IN, OUT)
-- =============================================================

ALTER TABLE inventory_movement
    DROP CONSTRAINT IF EXISTS inventory_movement_movement_type_check;

ALTER TABLE inventory_movement
    ADD CONSTRAINT inventory_movement_movement_type_check
        CHECK (movement_type IN ('RESTOCK', 'SALE', 'RETURN', 'ADJUSTMENT'));
