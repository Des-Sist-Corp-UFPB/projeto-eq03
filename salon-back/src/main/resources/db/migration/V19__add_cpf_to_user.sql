-- Adiciona campo CPF opcional (único) ao cadastro de usuário.
-- Coletado sob demanda (JIT) apenas no momento do pagamento via PIX.
ALTER TABLE tb_user ADD COLUMN cpf VARCHAR(14) UNIQUE;
