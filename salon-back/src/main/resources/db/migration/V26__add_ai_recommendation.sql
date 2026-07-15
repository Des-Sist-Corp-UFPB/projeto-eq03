-- V26__add_ai_recommendation.sql
-- Cache do último resultado gerado por tipo de recomendação, para não chamar o LLM
-- a cada vez que alguém abre a tela — só quando o usuário pedir explicitamente.

CREATE TABLE tb_ai_recommendation (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    payload TEXT NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_recommendation_type_generated ON tb_ai_recommendation (type, generated_at DESC);
