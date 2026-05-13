-- ============================================================================
-- V3__inserir_usuario_admin.sql
-- ============================================================================
-- Insere o usuário administrador do sistema
-- Senha: admin123 (hash BCrypt gerado com: new BCryptPasswordEncoder().encode("admin123"))
-- ============================================================================

INSERT INTO usuario (matricula, nome_completo, senha, email, ativo, created_at, updated_at)
VALUES (
    'admin',
    'Administrador do Sistema',
    '$2b$10$8AIxCFWzuAfs3p2/C0HDremJ52ZKmE1VR3NHcEIwnwnUDg0MAx57y',
    'admin@chamados.ufpb.br',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (matricula) DO UPDATE SET
    senha = EXCLUDED.senha,
    updated_at = CURRENT_TIMESTAMP;
