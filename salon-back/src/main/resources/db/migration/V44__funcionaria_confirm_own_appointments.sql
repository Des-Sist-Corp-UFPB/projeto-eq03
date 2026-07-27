-- V44__funcionaria_confirm_own_appointments.sql
--
-- A funcionária passa a poder definir o horário e recusar os agendamentos EM QUE ELA É A
-- PROFISSIONAL ATRIBUÍDA. Até aqui ela não conseguia fazer isso nem nos próprios atendimentos:
-- AppointmentService.confirm()/decline() exigiam ADMIN ou GERENTE_DE_ATENDIMENTO.
--
-- Esta migration concede apenas o acesso ao ENDPOINT. O recorte por dono é feito no serviço
-- (AppointmentService.assertCanManage), porque "o agendamento é dela?" depende da linha, e o
-- modelo de permissões deste projeto trabalha em cima de endpoint + método HTTP, não de
-- instância. As duas camadas juntas: o cargo abre a porta, o serviço confere de quem é a sala.
--
-- Vale notar o contrário disso, que também foi corrigido no serviço: a permissão de
-- PATCH /v1/appointments/*/status já havia sido concedida à FUNCIONARIA na V24 sem nenhuma
-- checagem de dono, ou seja, ela podia alterar o status do atendimento de qualquer colega.

INSERT INTO tb_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r, tb_permission p
WHERE r.name = 'FUNCIONARIA'
  AND p.http_method = 'PATCH'
  AND p.endpoint IN ('/v1/appointments/*/confirm', '/v1/appointments/*/decline')
  AND NOT EXISTS (
      SELECT 1 FROM tb_role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
