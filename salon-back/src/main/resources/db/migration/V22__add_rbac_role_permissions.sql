-- V22__add_rbac_role_permissions.sql
-- Adiciona permissões para os endpoints RBAC (GET /v1/roles, GET /v1/roles/permissions,
-- POST/DELETE /v1/roles/{roleId}/permissions/{permissionId})
-- Vincula apenas ao SYSADMIN. Idempotente com ON CONFLICT DO NOTHING.

-- 1) Inserir as novas permissões (idempotente)
INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Listar Roles com Permissões', '/v1/roles', 'GET', 'RBAC'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/roles' AND http_method = 'GET'
);

INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Listar Todas as Permissões', '/v1/roles/permissions', 'GET', 'RBAC'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/roles/permissions' AND http_method = 'GET'
);

INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Conceder Permissão a Role', '/v1/roles/{roleId}/permissions/{permissionId}', 'POST', 'RBAC'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/roles/{roleId}/permissions/{permissionId}' AND http_method = 'POST'
);

INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Revogar Permissão de Role', '/v1/roles/{roleId}/permissions/{permissionId}', 'DELETE', 'RBAC'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/roles/{roleId}/permissions/{permissionId}' AND http_method = 'DELETE'
);

-- 2) Vincular as novas permissões ao SYSADMIN (idempotente)
INSERT INTO tb_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r, tb_permission p
WHERE r.name = 'SYSADMIN'
  AND p.endpoint IN (
      '/v1/roles',
      '/v1/roles/permissions',
      '/v1/roles/{roleId}/permissions/{permissionId}'
  )
  AND NOT EXISTS (
      SELECT 1 FROM tb_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
