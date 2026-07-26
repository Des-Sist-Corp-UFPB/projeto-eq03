-- Perfil do salão e horário de funcionamento são conteúdo de negócio da própria dona do salão
-- (ADMIN), não configuração técnica de infraestrutura — por isso ficam em /v1/admin/salon/profile
-- e não em /v1/sysadmin/*. Sem grant pra nenhuma role: ADMIN e SYSADMIN já passam pelo bypass
-- automático do VerifyUserPermissions; GERENTE_DE_ATENDIMENTO não deveria editar isso.
INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Atualizar Perfil do Salão', '/v1/admin/salon/profile', 'PUT', 'SalonProfile'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/admin/salon/profile' AND http_method = 'PUT'
);
