-- Central de E-mails (tb_email_outbox). Leitura liberada para GERENTE_DE_ATENDIMENTO (além do
-- bypass automático de ADMIN/SYSADMIN); reenvio manual fica restrito a ADMIN/SYSADMIN (nenhuma
-- permissão concedida a GERENTE_DE_ATENDIMENTO para o POST de resend).

INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Listar Central de E-mails', '/v1/email-outbox', 'GET', 'CentralDeEmails'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/email-outbox' AND http_method = 'GET'
);

INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Reenviar E-mail Manualmente', '/v1/email-outbox/*/resend', 'POST', 'CentralDeEmails'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/email-outbox/*/resend' AND http_method = 'POST'
);

INSERT INTO tb_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r, tb_permission p
WHERE r.name = 'GERENTE_DE_ATENDIMENTO'
  AND p.http_method = 'GET'
  AND p.endpoint = '/v1/email-outbox'
  AND NOT EXISTS (
      SELECT 1 FROM tb_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
