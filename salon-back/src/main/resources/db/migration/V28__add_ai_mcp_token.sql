-- V28__add_ai_mcp_token.sql
-- Tokens de acesso ao servidor MCP (Fase 5) — múltiplos tokens nomeados e revogáveis
-- individualmente (padrão PAT do GitHub/Stripe), em vez de um segredo único compartilhado.
-- Só o hash é armazenado; o valor em texto puro só existe no momento da geração.

CREATE TABLE tb_ai_mcp_token (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    token_hash VARCHAR(100) NOT NULL UNIQUE,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_ai_mcp_token_hash ON tb_ai_mcp_token (token_hash);

INSERT INTO tb_permission (name, endpoint, http_method, classe) VALUES
    ('Listar Tokens MCP', '/v1/sysadmin/ai-config/tokens', 'GET', 'IA'),
    ('Gerar Token MCP', '/v1/sysadmin/ai-config/tokens', 'POST', 'IA'),
    ('Revogar Token MCP', '/v1/sysadmin/ai-config/tokens/*', 'DELETE', 'IA');
