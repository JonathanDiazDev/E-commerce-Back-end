-- 1. Categorías
INSERT INTO category (name, active) VALUES ('Electrónica', true);
INSERT INTO category (name, active) VALUES ('Hogar', true);

-- 2. Productos (Asegúrate de que no haya saltos de línea antes del VALUES)
INSERT INTO products (name, description, price, status, active, category_id, created_at) VALUES ('Laptop Pro', 'Potente laptop para desarrollo', 1500.00, 'ACTIVE', true, 1, NOW());
INSERT INTO products (name, description, price, status, active, category_id, created_at) VALUES ('Monitor 4K', 'Monitor ultra HD 27 pulgadas', 400.00, 'ACTIVE', true, 1, NOW());
INSERT INTO products (name, description, price, status, active, category_id, created_at) VALUES ('Cafetera Express', 'Cafetera automática de acero', 120.00, 'ACTIVE', true, 2, NOW());

-- 3. Inventario
INSERT INTO inventory (product_id, quantity, inventory_status, manual_disabled) VALUES (1, 10, 'IN_STOCK', false);
INSERT INTO inventory (product_id, quantity, inventory_status, manual_disabled) VALUES (2, 5, 'IN_STOCK', false);
INSERT INTO inventory (product_id, quantity, inventory_status, manual_disabled) VALUES (3, 0, 'OUT_OF_STOCK', false);