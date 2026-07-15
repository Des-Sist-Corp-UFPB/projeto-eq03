-- V25__add_ai_config_and_call_log.sql
-- Central de IA (Sysadmin): configuração do provedor LiteLLM (linha única) e log de chamadas,
-- usado tanto pelo motor de recomendações quanto para orçamento/rate limit.

CREATE TABLE tb_ai_config (
    id BIGINT PRIMARY KEY,
    base_url VARCHAR(255) NOT NULL,
    model VARCHAR(50) NOT NULL,
    api_key_encrypted TEXT,
    temperature NUMERIC(3, 2) NOT NULL DEFAULT 0.30,
    max_tokens INT NOT NULL DEFAULT 500,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    daily_call_budget INT NOT NULL DEFAULT 200,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Linha única (singleton) — id fixo em 1, nunca outra linha é criada.
INSERT INTO tb_ai_config (id, base_url, model, api_key_encrypted, temperature, max_tokens, enabled, daily_call_budget, updated_by, updated_at)
VALUES (1, 'https://llm.rodrigor.com', 'gpt-4o-mini', NULL, 0.30, 500, FALSE, 200, 'SYSTEM', now());

CREATE TABLE tb_ai_call_log (
    id BIGSERIAL PRIMARY KEY,
    caller_type VARCHAR(20) NOT NULL,
    caller_id VARCHAR(100),
    call_type VARCHAR(50) NOT NULL,
    tokens_used INT,
    latency_ms INT,
    success BOOLEAN NOT NULL,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_call_log_caller_created ON tb_ai_call_log (caller_type, caller_id, created_at);

-- Permissões: só SYSADMIN administra a Central de IA.
INSERT INTO tb_permission (name, endpoint, http_method, classe) VALUES
    ('Ver Configuração de IA', '/v1/sysadmin/ai-config', 'GET', 'Configuração'),
    ('Atualizar Configuração de IA', '/v1/sysadmin/ai-config', 'PUT', 'Configuração');

INSERT INTO tb_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r, tb_permission p
WHERE r.name = 'SYSADMIN' AND p.endpoint IN ('/v1/sysadmin/ai-config');
