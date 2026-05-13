CREATE TABLE usuario (
    id BIGSERIAL PRIMARY KEY,
    matricula VARCHAR(255) NOT NULL UNIQUE,
    nome_completo VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_usuario_matricula ON usuario(matricula);
