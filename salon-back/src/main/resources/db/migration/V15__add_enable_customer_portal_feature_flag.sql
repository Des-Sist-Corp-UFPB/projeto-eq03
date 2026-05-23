-- Seed ENABLE_CUSTOMER_PORTAL feature flag
INSERT INTO tb_feature_flag (name, enabled, description) VALUES
    ('ENABLE_CUSTOMER_PORTAL', FALSE, 'Ativa ou desativa a área pública de clientes');
