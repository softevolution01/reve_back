-- ==================================================================
-- 1. INSERTAR ROLES
-- ==================================================================
INSERT INTO roles (name) VALUES
('Administrador'),
('Empleado de Tienda')
ON CONFLICT (name) DO NOTHING;

-- ==================================================================
-- 2. INSERTAR PERMISOS
-- ==================================================================
INSERT INTO permissions (name) VALUES
('menu:catalog:access'),
('menu:inventory:access'),
('menu:sales:access'),
('menu:settings:access'),
('menu:dashboard:access'),
('sale:create'),
('catalog:edit'),
('catalog:delete'),
('catalog:create'),
('catalog:read:all'),
('catalog:read:detail'),
('sales:create:client'),
('inventory:edit:quantity')
ON CONFLICT (name) DO NOTHING;

-- ==================================================================
-- 3. ASIGNAR PERMISOS A ROLES
-- ==================================================================

-- Administrador: todos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Administrador'
  AND p.name IN (
        'menu:catalog:access', 'menu:inventory:access', 'menu:settings:access',
        'menu:dashboard:access', 'catalog:read:all','catalog:read:detail',
        'catalog:edit','catalog:delete','catalog:create','inventory:edit:quantity'
        )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Subadministrador: todos menos delete
---INSERT INTO role_permissions (role_id, permission_id)
---SELECT r.id, p.id
---FROM roles r, permissions p
---WHERE r.name = 'Subadministrador'
---  AND p.name IN (
---        'menu:catalog:access', 'menu:inventory:access', 'menu:settings:access',
---        'menu:dashboard:access'
---      )
---ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Empleado de Tienda
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Empleado de Tienda'
  AND p.name IN (
            'menu:sales:access', 'menu:inventory:access','catalog:read:all', 'sale:create','sales:create:client','inventory:edit:quantity'
          )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Asesor de Ventas
--INSERT INTO role_permissions (role_id, permission_id)
--SELECT r.id, p.id
--FROM roles r, permissions p
--WHERE r.name = 'Asesor de Ventas'
--  AND p.name IN ('menu:sales:access', 'sale:create')
--ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Cliente
--INSERT INTO role_permissions (role_id, permission_id)
---SELECT r.id, p.id
-- roles r, permissions p
--WHERE r.name = 'Cliente'
--  AND p.name = 'catalog:read:all'
--ON CONFLICT (role_id, permission_id) DO NOTHING;


-- ==================================================================
-- 4. SUCURSALES
-- ==================================================================

--INSERT INTO warehouses (name, location)
--VALUES ('ALMACEN PRUEBA', 'Arequipa - Centro');

--INSERT INTO branches (name, location, warehouse_id, is_cash_managed_centralized)
--VALUES (
--    'SUCURSAL PRUEBA 1',
--    'Arequipa - Calle Principal',
--    (SELECT id FROM warehouses WHERE name = 'ALMACEN PRUEBA'),
--    FALSE
--);

-- SUCURSAL AQP 1 (Sucursal legal que envía su efectivo a la Principal)
--INSERT INTO branches (name, location, warehouse_id, is_cash_managed_centralized)
--VALUES (
--    'SUCURSAL PRUEBA 2',
--    'Arequipa - Zona Norte',
--    (SELECT id FROM warehouses WHERE name = 'ALMACEN PRUEBA'),
--    TRUE
--);

--INSERT INTO warehouses (name, location)
--VALUES ('REVE CENTRAL AQP', 'Arequipa - Centro');


--INSERT INTO branches (name, location, warehouse_id, is_cash_managed_centralized)
--VALUES (
--    'SUCURSAL REVE AQP',
--    'Arequipa - Calle Principal',
--    (SELECT id FROM warehouses WHERE name = 'REVE CENTRAL AQP'),
--    FALSE
--);

-- SUCURSAL AQP 1 (Sucursal legal que envía su efectivo a la Principal)
--INSERT INTO branches (name, location, warehouse_id, is_cash_managed_centralized)
--VALUES (
--    'SUCURSAL AQP 1',
--    'Arequipa - Zona Norte',
--    (SELECT id FROM warehouses WHERE name = 'REVE CENTRAL AQP'),
--    TRUE
--);

-- SUCURSAL AQP 2 (Sucursal legal que envía su efectivo a la Principal)
--INSERT INTO branches (name, location, warehouse_id, is_cash_managed_centralized)
--VALUES (
--    'SUCURSAL AQP 2',
--    'Arequipa - Zona Sur',
--    (SELECT id FROM warehouses WHERE name = 'REVE CENTRAL AQP'),
--    TRUE
--);