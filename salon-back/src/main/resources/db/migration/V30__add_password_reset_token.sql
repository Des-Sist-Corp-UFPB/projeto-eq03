-- V30__add_password_reset_token.sql
-- Tokens de redefinição de senha via e-mail (fluxo "esqueci minha senha"). Só o hash é
-- armazenado; o valor em texto puro só existe no e-mail enviado ao usuário, nunca no banco
-- (mesmo padrão de tb_ai_mcp_token). expires_at é curto (30 min) e used_at marca uso único.

CREATE TABLE tb_password_reset_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES tb_user(id),
    token_hash VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP
);

CREATE INDEX idx_password_reset_token_hash ON tb_password_reset_token (token_hash);
