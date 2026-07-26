-- Fila de e-mails com retry automático (backoff) e histórico curto de envio recente.
--
-- Não substitui tb_audit_log — auditoria continua sendo o registro permanente de todo envio
-- (ação EMAIL_SENT, SUCCESS/FAILURE). Esta tabela tem um propósito operacional diferente e
-- deliberadamente mais estreito: saber o que ainda falta reenviar, e dar visibilidade de curto
-- prazo pro admin conferir/forçar reenvio. Por isso a retenção é curta (ver job de limpeza em
-- EmailOutboxService) — LGPD (minimização/finalidade): manter o corpo do e-mail e o endereço
-- do destinatário além do necessário pra esse propósito não se justifica.
CREATE TABLE tb_email_outbox (
    id BIGSERIAL PRIMARY KEY,

    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    html_content TEXT NOT NULL,
    reply_to VARCHAR(255),

    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    last_error VARCHAR(500),

    -- Contexto de negócio pra exibir na tela do admin (ex.: "Appointment" / 42), sem precisar
    -- decodificar o assunto do e-mail.
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_email_outbox_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'DEAD_LETTER'))
);

-- Usado pelo job de retry (busca FAILED com next_retry_at vencido) e pelo filtro da tela admin.
CREATE INDEX idx_email_outbox_status_next_retry ON tb_email_outbox(status, next_retry_at);
-- Usado pelo job de limpeza (SENT antigos, DEAD_LETTER antigos).
CREATE INDEX idx_email_outbox_status_updated_at ON tb_email_outbox(status, updated_at);
