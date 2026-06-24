-- Remove CPF unique constraint and clear existing CPFs to prevent any constraint conflict
UPDATE tb_user SET cpf = NULL;
ALTER TABLE tb_user DROP CONSTRAINT IF EXISTS tb_user_cpf_key;
