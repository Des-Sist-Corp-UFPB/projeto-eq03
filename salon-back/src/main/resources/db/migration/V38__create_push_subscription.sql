-- Notificações push via Web Push API (issue #110). Guarda a subscription que o navegador
-- devolve ao autorizar notificações — endpoint (URL do serviço de push do navegador,
-- ex.: FCM/Mozilla autopush) + as duas chaves públicas que o backend usa para cifrar o payload
-- (p256dh/auth), conforme o protocolo Web Push padrão. Nenhum dado pessoal sensível aqui além
-- do vínculo com o usuário — o endpoint em si não identifica a pessoa sozinho.
CREATE TABLE tb_push_subscription (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES tb_user(id) ON DELETE CASCADE,
    endpoint TEXT NOT NULL,
    p256dh TEXT NOT NULL,
    auth TEXT NOT NULL,
    user_agent VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Um mesmo navegador/dispositivo não deveria gerar duas subscriptions para o mesmo usuário.
    CONSTRAINT uk_push_subscription_user_endpoint UNIQUE (user_id, endpoint)
);

CREATE INDEX idx_push_subscription_user ON tb_push_subscription(user_id);
