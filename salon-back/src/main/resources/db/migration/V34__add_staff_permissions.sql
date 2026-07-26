-- V34__add_staff_permissions.sql
-- Cadastro completo de equipe (tb_staff_profile). A criação (POST) e a geração de QR Code
-- PIX são restritas a ADMIN/SYSADMIN de propósito (decisão de produto): nenhuma permissão é
-- concedida a GERENTE_DE_ATENDIMENTO para estes dois métodos, então só passam pelas regras
-- 1/2 do VerifyUserPermissions (bypass de SYSADMIN e ADMIN). GET é liberado para
-- GERENTE_DE_ATENDIMENTO, que já gerencia a tela de Equipe hoje.

INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Criar Cadastro de Equipe', '/v1/staff', 'POST', 'Equipe'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/staff' AND http_method = 'POST'
);

INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Listar Cadastros de Equipe', '/v1/staff', 'GET', 'Equipe'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/staff' AND http_method = 'GET'
);

INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Ver Cadastro de Equipe', '/v1/staff/*', 'GET', 'Equipe'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/staff/*' AND http_method = 'GET'
);

INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Gerar QR Code PIX de Equipe', '/v1/staff/*/pix-qrcode', 'POST', 'Equipe'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/staff/*/pix-qrcode' AND http_method = 'POST'
);

-- Só as permissões de leitura vão para GERENTE_DE_ATENDIMENTO. POST (criar cadastro e gerar
-- QR de pagamento) fica de fora — só ADMIN/SYSADMIN têm acesso a essas duas ações.
INSERT INTO tb_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r, tb_permission p
WHERE r.name = 'GERENTE_DE_ATENDIMENTO'
  AND p.http_method = 'GET'
  AND p.endpoint IN ('/v1/staff', '/v1/staff/*')
  AND NOT EXISTS (
      SELECT 1 FROM tb_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
