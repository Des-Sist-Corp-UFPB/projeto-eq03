-- Qualquer usuário autenticado pode se inscrever/desinscrever de push — não é uma ação
-- administrativa, é o próprio usuário controlando sua própria notificação. ADMIN/SYSADMIN já
-- passam por bypass automático do VerifyUserPermissions; concede explicitamente para os demais.

INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Assinar Notificações Push', '/v1/push/subscribe', 'POST', 'Push'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/push/subscribe' AND http_method = 'POST'
);

INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Cancelar Notificações Push', '/v1/push/unsubscribe', 'DELETE', 'Push'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/push/unsubscribe' AND http_method = 'DELETE'
);

INSERT INTO tb_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r, tb_permission p
WHERE r.name IN ('CLIENTE', 'FUNCIONARIA', 'GERENTE_DE_ATENDIMENTO')
  AND p.endpoint IN ('/v1/push/subscribe', '/v1/push/unsubscribe')
  AND NOT EXISTS (
      SELECT 1 FROM tb_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
