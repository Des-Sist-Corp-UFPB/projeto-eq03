-- Seed SYSADMIN role
INSERT INTO tb_role (name) VALUES ('SYSADMIN');

-- Seed SYSADMIN permissions
INSERT INTO tb_permission (name, endpoint, http_method, classe) VALUES
    ('Listar Feature Flags', '/v1/sysadmin/feature-flags', 'GET', 'Configuração'),
    ('Alternar Feature Flags', '/v1/sysadmin/feature-flags/*/toggle', 'PATCH', 'Configuração'),
    ('Ver Auditoria', '/v1/audit/**', 'GET', 'Auditoria');

-- Bind permissions to SYSADMIN role
INSERT INTO tb_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM tb_role r, tb_permission p
WHERE r.name = 'SYSADMIN' AND p.endpoint IN (
    '/v1/sysadmin/feature-flags', '/v1/sysadmin/feature-flags/*/toggle', '/v1/audit/**'
);

-- Seed default SYSADMIN user (password: SysAdmin@123)
INSERT INTO tb_user (name, email, password, role_id)
SELECT
    'System Administrator',
    'sysadmin@salao.com',
    '$2b$10$iIVwc0WRvpPcKSt6vV6FVe5ZFz0esL1.y5a/zzLDUV7NpdpNTBvNK',
    r.id
FROM tb_role r WHERE r.name = 'SYSADMIN';

-- Create table tb_feature_flag
CREATE TABLE tb_feature_flag (
    name VARCHAR(100) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(255)
);

-- Seed default feature flags
INSERT INTO tb_feature_flag (name, enabled, description) VALUES
    ('EMAIL_NOTIFICATIONS', FALSE, 'Habilita o envio de e-mails de notificação'),
    ('CLIENT_BOOKING', TRUE, 'Permite que os clientes realizem agendamentos');
