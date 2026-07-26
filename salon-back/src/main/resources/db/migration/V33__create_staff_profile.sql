-- Cadastro completo da equipe (FUNCIONARIA / GERENTE_DE_ATENDIMENTO).
--
-- Fica numa tabela própria (e não em tb_user/tb_employee) por dois motivos:
--   1. GERENTE_DE_ATENDIMENTO não é Employee hoje — Employee guarda só remuneração/bio.
--   2. Isola os dados pessoais sensíveis, permitindo permissão e auditoria mais restritas
--      sobre esta tabela sem afetar o resto do sistema.
--
-- Campos sensíveis (cpf, chave PIX) são gravados CIFRADOS pela aplicação (AES-256-GCM,
-- ver EncryptedStringConverter) — por isso são TEXT e não têm formato validável no banco.
-- A chave mestra vive só na env APP_PII_ENCRYPTION_KEY: um dump do banco sozinho não expõe nada.

CREATE TABLE tb_staff_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES tb_user(id) ON DELETE CASCADE,

    -- Dados pessoais
    full_name VARCHAR(150) NOT NULL,
    social_name VARCHAR(150),
    -- Cifrado. Não dá para indexar/buscar — a checagem de duplicidade usa cpf_hash.
    cpf_encrypted TEXT NOT NULL,
    -- HMAC-SHA256 (pepper na env) do CPF só com dígitos: permite UNIQUE sem decifrar.
    cpf_hash VARCHAR(64) NOT NULL UNIQUE,
    birth_date DATE NOT NULL,
    gender VARCHAR(30),

    -- Contato
    phone VARCHAR(20) NOT NULL,
    emergency_contact_name VARCHAR(150),
    emergency_contact_phone VARCHAR(20),

    -- Endereço (em claro de propósito: precisa ser filtrável/relatável e não é dado sensível LGPD)
    zip_code VARCHAR(9) NOT NULL,
    street VARCHAR(200) NOT NULL,
    street_number VARCHAR(20) NOT NULL,
    complement VARCHAR(100),
    district VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state_uf VARCHAR(2) NOT NULL,

    -- Recebimento via PIX. A chave é cifrada e NUNCA volta em claro pela API:
    -- para pagar, o backend decifra em memória e devolve só o payload do QR Code.
    pix_key_type VARCHAR(20),
    pix_key_encrypted TEXT,
    -- Máscara pré-calculada (ex.: "joa•••••@mail.com") para exibir na UI sem decifrar nada.
    pix_key_masked VARCHAR(120),

    -- Metadados
    hired_at DATE,
    notes TEXT,
    created_by_user_id BIGINT REFERENCES tb_user(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Se há tipo de chave PIX, tem que haver chave cifrada — e vice-versa.
    CONSTRAINT chk_staff_profile_pix_pair CHECK (
        (pix_key_type IS NULL AND pix_key_encrypted IS NULL)
        OR (pix_key_type IS NOT NULL AND pix_key_encrypted IS NOT NULL)
    ),
    CONSTRAINT chk_staff_profile_state_uf CHECK (state_uf ~ '^[A-Z]{2}$'),
    CONSTRAINT chk_staff_profile_birth_past CHECK (birth_date < CURRENT_DATE)
);

CREATE INDEX idx_staff_profile_user_id ON tb_staff_profile(user_id);
CREATE INDEX idx_staff_profile_city ON tb_staff_profile(city);
