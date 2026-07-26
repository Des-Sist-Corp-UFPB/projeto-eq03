-- Horário de funcionamento por dia da semana (issue #116). Sempre 7 linhas fixas (uma por
-- DayOfWeek do java.time) — a aplicação nunca insere/remove linha, só atualiza is_open/
-- open_time/close_time. Isso evita ter que tratar "dia sem configuração" em lugar nenhum.
CREATE TABLE tb_business_hours (
    id BIGSERIAL PRIMARY KEY,
    day_of_week VARCHAR(10) NOT NULL UNIQUE,
    is_open BOOLEAN NOT NULL DEFAULT TRUE,
    open_time TIME,
    close_time TIME,

    CONSTRAINT chk_business_hours_day CHECK (day_of_week IN
        ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    -- Se aberto, os dois horários são obrigatórios e a abertura precisa vir antes do fechamento.
    CONSTRAINT chk_business_hours_times CHECK (
        (is_open = FALSE) OR (open_time IS NOT NULL AND close_time IS NOT NULL AND open_time < close_time)
    )
);

-- Semente neutra (todos os dias abertos 08:00-18:00) — só um ponto de partida editável pelo
-- admin, não reflete necessariamente o horário real do salão.
INSERT INTO tb_business_hours (day_of_week, is_open, open_time, close_time) VALUES
    ('MONDAY', TRUE, '08:00', '18:00'),
    ('TUESDAY', TRUE, '08:00', '18:00'),
    ('WEDNESDAY', TRUE, '08:00', '18:00'),
    ('THURSDAY', TRUE, '08:00', '18:00'),
    ('FRIDAY', TRUE, '08:00', '18:00'),
    ('SATURDAY', TRUE, '08:00', '18:00'),
    ('SUNDAY', FALSE, NULL, NULL);
