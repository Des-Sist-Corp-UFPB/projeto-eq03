-- V21__add_client_permissions_and_update_endpoints.sql
-- Adiciona permissões específicas para Clientes se não existirem (Idempotente)
INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Listar Clientes', '/v1/clients', 'GET', 'Cliente'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/clients' AND http_method = 'GET'
);

INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Ver Detalhes do Cliente', '/v1/clients/*', 'GET', 'Cliente'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/clients/*' AND http_method = 'GET'
);

-- Vincula permissões à role GERENTE_DE_ATENDIMENTO se não estiverem vinculadas (Idempotente)
INSERT INTO tb_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r, tb_permission p
WHERE r.name = 'GERENTE_DE_ATENDIMENTO'
  AND p.endpoint IN ('/v1/clients', '/v1/clients/*')
  AND NOT EXISTS (
      SELECT 1 FROM tb_role_permissions rp 
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
