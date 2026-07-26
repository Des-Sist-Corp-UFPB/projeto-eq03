-- Um agendamento passa a poder ter múltiplos serviços em vez de só um.
-- Cria a tabela de itens de serviço por agendamento, migra os dados existentes
-- (um item por agendamento, preservando o serviço e as customizações atuais)
-- e remove as colunas antigas de tb_appointment.

CREATE TABLE tb_appointment_service_item (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL REFERENCES tb_appointment(id) ON DELETE CASCADE,
    salon_service_id BIGINT NOT NULL REFERENCES tb_salon_service(id),
    custom_price DECIMAL(10, 2),
    custom_duration_min INTEGER,
    custom_service_notes TEXT
);

INSERT INTO tb_appointment_service_item (appointment_id, salon_service_id, custom_price, custom_duration_min, custom_service_notes)
SELECT id, salon_service_id, custom_price, custom_duration_min, custom_service_notes
FROM tb_appointment;

ALTER TABLE tb_appointment
    DROP COLUMN salon_service_id,
    DROP COLUMN custom_price,
    DROP COLUMN custom_duration_min,
    DROP COLUMN custom_service_notes;
