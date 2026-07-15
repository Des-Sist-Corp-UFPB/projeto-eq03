-- V27__add_ai_recommendations_permissions.sql
-- Endpoints da tela de Recomendações de IA (Fase 3). ADMIN já tem acesso total via
-- @verifyUserPermissions; só GERENTE_DE_ATENDIMENTO precisa de vínculo explícito.

INSERT INTO tb_permission (name, endpoint, http_method, classe) VALUES
    ('Ver Recomendações de IA', '/v1/admin/recommendations/*', 'GET', 'IA'),
    ('Gerar Recomendações de IA', '/v1/admin/recommendations/*/generate', 'POST', 'IA');

INSERT INTO tb_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r, tb_permission p
WHERE r.name = 'GERENTE_DE_ATENDIMENTO' AND p.endpoint IN (
    '/v1/admin/recommendations/*', '/v1/admin/recommendations/*/generate'
);
