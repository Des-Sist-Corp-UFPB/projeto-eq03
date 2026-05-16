-- Preço do serviço opcional (valor exato entra no caixa ao concluir o atendimento)
ALTER TABLE tb_salon_service ALTER COLUMN price DROP NOT NULL;

-- Solicitação de agenda: horário definido pela profissional após aceitar
ALTER TABLE tb_appointment ALTER COLUMN scheduled_at DROP NOT NULL;

ALTER TABLE tb_appointment ADD COLUMN preferred_date DATE;
ALTER TABLE tb_appointment ADD COLUMN client_notes TEXT;

ALTER TABLE tb_appointment ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
