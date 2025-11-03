-- ==================================================================
-- 1. INSERTAR ROLES (exactamente como en el PDF)
-- ==================================================================
INSERT INTO roles (name) VALUES
('Administrador'),
('Subadministrador'),
('Empleado de Tienda'),
('Asesor de Ventas'),
('Cliente')
ON CONFLICT (name) DO NOTHING;

-- ==================================================================
-- 2. INSERTAR PERMISOS (exactamente como en el PDF)
-- ==================================================================
INSERT INTO permissions (name) VALUES
('menu:sales:access'),
('menu:inventory:access'),
('sale:create'),
('product:read:all');

-- ==================================================================
-- 3. ASIGNAR PERMISOS A ROLES (según lógica del PDF)
-- ==================================================================

-- Administrador: todos los permisos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Administrador'
  AND p.name IN ('menu:sales:access', 'menu:inventory:access', 'sale:create', 'product:read:all')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Subadministrador: todos menos (por defecto, igual que admin en este caso)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Subadministrador'
  AND p.name IN ('menu:sales:access', 'menu:inventory:access', 'sale:create', 'product:read:all')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Empleado de Tienda: ventas e inventario
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Empleado de Tienda'
  AND p.name IN ('menu:sales:access', 'menu:inventory:access', 'sale:create', 'product:read:all')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Asesor de Ventas: solo ventas
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Asesor de Ventas'
  AND p.name IN ('menu:sales:access', 'sale:create')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Cliente: solo lectura
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Cliente'
  AND p.name = 'product:read:all'
ON CONFLICT (role_id, permission_id) DO NOTHING;