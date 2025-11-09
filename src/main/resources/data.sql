-- ==================================================================
-- 1. INSERTAR ROLES
-- ==================================================================
INSERT INTO roles (name) VALUES
('Administrador'),
('Subadministrador'),
('Empleado de Tienda'),
('Asesor de Ventas'),
('Cliente')
ON CONFLICT (name) DO NOTHING;

-- ==================================================================
-- 2. INSERTAR PERMISOS
-- ==================================================================
INSERT INTO permissions (name) VALUES
('menu:sales:access'),
('menu:inventory:access'),
('sale:create'),
('inventory:edit'),
('inventory:delete'),
('inventory:create'),
('menu:settings:access'),
('menu:dashboard:access'),
('product:read:all')
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
        'menu:sales:access', 'menu:inventory:access', 'sale:create',
        'inventory:edit', 'inventory:delete', 'inventory:create',
        'menu:settings:access', 'menu:dashboard:access', 'product:read:all'
      )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Subadministrador: todos menos delete
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Subadministrador'
  AND p.name IN (
        'menu:sales:access', 'menu:inventory:access', 'sale:create',
        'inventory:edit', 'inventory:create',
        'menu:settings:access', 'menu:dashboard:access', 'product:read:all'
      )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Empleado de Tienda
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Empleado de Tienda'
  AND p.name IN (
            'menu:sales:access', 'menu:inventory:access', 'sale:create',
            'inventory:create', 'product:read:all'
          )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Asesor de Ventas
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Asesor de Ventas'
  AND p.name IN ('menu:sales:access', 'sale:create', 'product:read:all')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Cliente
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Cliente'
  AND p.name = 'product:read:all'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ==================================================================
-- 4. SUCURSALES
-- ==================================================================
INSERT INTO branches (name, location)
SELECT 'Sede Principal CC', 'Arequipa'
WHERE NOT EXISTS (SELECT 1 FROM branches WHERE name = 'Sede Principal CC');

INSERT INTO branches (name, location)
SELECT 'Sede Plaza de Armas', 'Arequipa'
WHERE NOT EXISTS (SELECT 1 FROM branches WHERE name = 'Sede Plaza de Armas');