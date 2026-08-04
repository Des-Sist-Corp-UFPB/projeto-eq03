-- Impede lançamento duplicado de receita no caixa para o mesmo agendamento. Antes disso a
-- checagem de "já faturado" era feita em memória (findAll + anyMatch) e não era atômica: dois
-- webhooks/requisições concorrentes para o mesmo agendamento podiam lançar a receita duas vezes.
-- NULL não conflita com NULL em uma UNIQUE constraint no Postgres, então isso não afeta
-- lançamentos que não estão ligados a um agendamento.
ALTER TABLE tb_cashflow ADD CONSTRAINT uk_cashflow_appointment_id UNIQUE (appointment_id);
