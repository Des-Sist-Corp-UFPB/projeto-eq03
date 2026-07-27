-- V43__machine_timestamps_to_timestamptz.sql
--
-- Separa os dois tipos de data que até aqui compartilhavam o mesmo tipo (TIMESTAMP sem fuso)
-- e o mesmo serializador — confusão que causou um bug visível em produção: um horário
-- escolhido às 22h na tela administrativa aparecia como 19h na listagem.
--
--   * Hora local do negócio  -> tb_appointment.scheduled_at e preferred_date
--     Continuam SEM fuso, de propósito. "14h" é hora de relógio de parede no endereço do
--     salão: não deve se mover se o servidor mudar de continente, nem se o país alterar sua
--     regra de fuso (o Brasil aboliu o horário de verão em 2019 — um agendamento futuro
--     guardado como instante teria "andado" no relógio quando isso aconteceu).
--     ESTAS COLUNAS NÃO SÃO TOCADAS AQUI.
--
--   * Instante de máquina   -> created_at, updated_at, sent_at, expires_at, ...
--     "Quando isso aconteceu" é um ponto real na linha do tempo, então vira TIMESTAMPTZ.
--
-- Sobre a conversão dos dados já gravados: a aplicação sempre rodou com a JVM em UTC (ver
-- SalonApplication.init()), então os valores existentes nessas colunas são hora de parede em
-- UTC. Por isso o USING ... AT TIME ZONE 'UTC': ele diz ao Postgres "leia este valor como se
-- fosse UTC", que é exatamente o que ele é. Não é um deslocamento fixo de -3h chapado — é uma
-- reinterpretação, e o Postgres aplica as regras históricas de fuso corretas.

-- tb_user
ALTER TABLE tb_user
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- tb_appointment (scheduled_at e preferred_date ficam de fora, ver comentário acima)
ALTER TABLE tb_appointment
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN reminded_at TYPE TIMESTAMPTZ USING reminded_at AT TIME ZONE 'UTC';

-- tb_audit_log
ALTER TABLE tb_audit_log
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- tb_email_outbox
ALTER TABLE tb_email_outbox
    ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at   TYPE TIMESTAMPTZ USING updated_at   AT TIME ZONE 'UTC',
    ALTER COLUMN sent_at      TYPE TIMESTAMPTZ USING sent_at      AT TIME ZONE 'UTC',
    ALTER COLUMN next_retry_at TYPE TIMESTAMPTZ USING next_retry_at AT TIME ZONE 'UTC';

-- tb_push_subscription
ALTER TABLE tb_push_subscription
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

-- tb_staff_profile
ALTER TABLE tb_staff_profile
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- tb_password_reset_token
ALTER TABLE tb_password_reset_token
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN expires_at TYPE TIMESTAMPTZ USING expires_at AT TIME ZONE 'UTC',
    ALTER COLUMN used_at    TYPE TIMESTAMPTZ USING used_at    AT TIME ZONE 'UTC';

-- tb_ai_config / tb_ai_call_log / tb_ai_recommendation / tb_ai_mcp_token
ALTER TABLE tb_ai_config
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE tb_ai_call_log
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE tb_ai_recommendation
    ALTER COLUMN generated_at TYPE TIMESTAMPTZ USING generated_at AT TIME ZONE 'UTC';

ALTER TABLE tb_ai_mcp_token
    ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC',
    ALTER COLUMN expires_at   TYPE TIMESTAMPTZ USING expires_at   AT TIME ZONE 'UTC',
    ALTER COLUMN last_used_at TYPE TIMESTAMPTZ USING last_used_at AT TIME ZONE 'UTC';

-- tb_salon_profile
ALTER TABLE tb_salon_profile
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';
