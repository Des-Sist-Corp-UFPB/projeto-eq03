-- Perfil público do salão (issue #117). Tabela singleton — só existe UM salão, então a
-- aplicação sempre trabalha com o registro de id mais baixo (não há FK de ninguém pra cá).
-- Nada aqui é dado sensível: tudo é exibido publicamente na página inicial.
CREATE TABLE tb_salon_profile (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    address VARCHAR(300),
    phone VARCHAR(20),
    instagram VARCHAR(150),
    whatsapp VARCHAR(20),
    logo_url VARCHAR(500),
    updated_at TIMESTAMP
);

-- Semente com valores neutros para o site nunca ficar sem nada pra mostrar antes do primeiro
-- preenchimento pelo admin.
INSERT INTO tb_salon_profile (name, description)
VALUES ('Espaço Cristiane Moura', 'Salão de beleza — agendamentos e gestão.');
