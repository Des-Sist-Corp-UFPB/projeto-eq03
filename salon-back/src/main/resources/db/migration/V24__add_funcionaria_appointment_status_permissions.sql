-- V24__add_funcionaria_appointment_status_permissions.sql
-- Os endpoints PATCH /v1/appointments/{id}/status e /payment-status já documentam
-- (ver AppointmentController) que GERENTE_DE_ATENDIMENTO e FUNCIONARIA podem executá-los,
-- mas a permissão de status nunca foi vinculada à FUNCIONARIA, e a de pagamento nunca
-- chegou a existir para nenhum cargo além do acesso total do ADMIN. Isso deixava a tela
-- administrativa de agendamentos sem nenhuma ação útil para quem não é ADMIN/GERENTE.

-- 1) Cria a permissão de atualização de status de pagamento (idempotente)
INSERT INTO tb_permission (name, endpoint, http_method, classe)
SELECT 'Atualizar Status de Pagamento', '/v1/appointments/*/payment-status', 'PATCH', 'Agendamento'
WHERE NOT EXISTS (
    SELECT 1 FROM tb_permission WHERE endpoint = '/v1/appointments/*/payment-status' AND http_method = 'PATCH'
);

-- 2) Vincula "Atualizar Status Agendamento" (já existente, faltava para FUNCIONARIA) e a nova
--    permissão de pagamento (faltava para GERENTE_DE_ATENDIMENTO e FUNCIONARIA)
INSERT INTO tb_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r, tb_permission p
WHERE r.name IN ('GERENTE_DE_ATENDIMENTO', 'FUNCIONARIA')
  AND p.http_method = 'PATCH'
  AND p.endpoint IN ('/v1/appointments/*/status', '/v1/appointments/*/payment-status')
  AND NOT EXISTS (
      SELECT 1 FROM tb_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
