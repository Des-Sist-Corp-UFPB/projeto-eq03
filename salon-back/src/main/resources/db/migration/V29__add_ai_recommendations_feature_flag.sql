-- V29__add_ai_recommendations_feature_flag.sql
-- "Ship dark": o módulo de IA (recomendações + servidor MCP) nasce desligado em produção,
-- independente da Central de IA já estar configurada — dá pra preparar tudo com calma e só
-- ligar quando validado, sem precisar de deploy pra isso.

INSERT INTO tb_feature_flag (name, enabled, description) VALUES
    ('ENABLE_AI_RECOMMENDATIONS', FALSE, 'Habilita o motor de recomendações de IA (tela admin e servidor MCP)');
