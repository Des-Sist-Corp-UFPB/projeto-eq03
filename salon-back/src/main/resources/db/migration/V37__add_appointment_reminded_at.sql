-- Lembrete de agendamento D-1 (issue #111). NULL = ainda não recebeu lembrete; marcado assim
-- que o job dispara o e-mail (não espera confirmação de entrega — isso é responsabilidade da
-- fila de retry do e-mail em si), para o job diário nunca reenviar pro mesmo agendamento.
ALTER TABLE tb_appointment ADD COLUMN reminded_at TIMESTAMP;

CREATE INDEX idx_appointment_reminder_lookup ON tb_appointment(status, scheduled_at) WHERE reminded_at IS NULL;
