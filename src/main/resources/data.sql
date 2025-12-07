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
('catalog:read:detail')
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
        'catalog:edit','catalog:delete','catalog:create'
        )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Subadministrador: todos menos delete
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Subadministrador'
  AND p.name IN (
        'menu:catalog:access', 'menu:inventory:access', 'menu:settings:access',
        'menu:dashboard:access'
      )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Empleado de Tienda
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Empleado de Tienda'
  AND p.name IN (
            'menu:sales:access', 'menu:inventory:access', 'sale:create'
          )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Asesor de Ventas
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Asesor de Ventas'
  AND p.name IN ('menu:sales:access', 'sale:create')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Cliente
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'Cliente'
  AND p.name = 'catalog:read:all'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ==================================================================
-- 4. SUCURSALES
-- ==================================================================
--INSERT INTO branches (name, location)
--SELECT 'Sede Principal CC', 'Arequipa'
--WHERE NOT EXISTS (SELECT 1 FROM branches WHERE name = 'Sede Principal CC');

INSERT INTO branches (name, location)
SELECT 'Sede Plaza de Armas', 'Arequipa'
WHERE NOT EXISTS (SELECT 1 FROM branches WHERE name = 'Sede Plaza de Armas');