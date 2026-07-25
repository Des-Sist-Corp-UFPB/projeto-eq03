-- V31__add_appointment_custom_service_fields.sql
-- O serviço cadastrado passa a funcionar como um template: estes campos sobrescrevem
-- preço/duração/observações só para um agendamento específico, sem alterar o cadastro
-- do serviço (ex.: "Coloração" custa R$150 no catálogo, mas R$200 pra uma cliente
-- específica com cabelo longo). Nulo = usa o valor do serviço normalmente.

ALTER TABLE tb_appointment
    ADD COLUMN custom_price DECIMAL(10, 2),
    ADD COLUMN custom_duration_min INTEGER,
    ADD COLUMN custom_service_notes TEXT;
