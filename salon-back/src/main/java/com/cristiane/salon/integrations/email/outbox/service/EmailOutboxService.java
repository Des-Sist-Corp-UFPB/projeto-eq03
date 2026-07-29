package com.cristiane.salon.integrations.email.outbox.service;

import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.integrations.email.outbox.dto.EmailOutboxFilter;
import com.cristiane.salon.integrations.email.outbox.dto.EmailOutboxResponse;
import com.cristiane.salon.integrations.email.outbox.entity.EmailOutboxEntry;
import com.cristiane.salon.integrations.email.outbox.enums.EmailOutboxStatus;
import com.cristiane.salon.integrations.email.outbox.repository.EmailOutboxRepository;
import com.cristiane.salon.integrations.email.outbox.specification.EmailOutboxSpecifications;
import com.cristiane.salon.integrations.email.service.EmailGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Fila de e-mails com retry automático (backoff em {@link EmailOutboxEntry#BACKOFF_MINUTES})
 * e limpeza periódica — ver a seção "Fila de e-mail (outbox) e retenção" no README para a
 * política completa (por quanto tempo cada status fica guardado e por quê).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOutboxService {

    private final EmailOutboxRepository repository;
    private final EmailGateway emailGateway;

    @Value("${app.email-outbox.sent-retention-days:7}")
    private int sentRetentionDays;

    @Value("${app.email-outbox.dead-letter-retention-days:90}")
    private int deadLetterRetentionDays;

    /**
     * Grava a tentativa e envia na hora. Se falhar, a exceção é relançada — o chamador
     * ({@link com.cristiane.salon.integrations.email.service.EmailService}) continua decidindo
     * o que fazer (hoje: registrar FAILURE no log de auditoria), exatamente como antes desta
     * fila existir. A diferença é que agora, além disso, fica registrado aqui para retry
     * automático mais tarde.
     *
     * {@code noRollbackFor}: sem isso, o rollback automático do Spring pra qualquer
     * RuntimeException que escapa de um método @Transactional desfaz também o
     * {@code repository.save(entry)} que acabou de gravar a falha — apagando, no mesmo
     * instante, o próprio registro que essa fila existe pra preservar. A entrada precisa
     * sobreviver à exceção que é relançada logo em seguida.
     */
    @Transactional(noRollbackFor = RuntimeException.class)
    public void sendNow(String to, String subject, String htmlContent, String replyTo,
            String relatedEntityType, Long relatedEntityId) {
        EmailOutboxEntry entry = EmailOutboxEntry.create(to, subject, htmlContent, replyTo,
                relatedEntityType, relatedEntityId);
        repository.save(entry);

        if (!attempt(entry)) {
            throw new RuntimeException(entry.getLastError());
        }
    }

    @Transactional(readOnly = true)
    public Page<EmailOutboxResponse> findAll(EmailOutboxFilter filter, Pageable pageable) {
        return repository.findAll(EmailOutboxSpecifications.filter(filter), pageable)
                .map(EmailOutboxResponse::fromEntity);
    }

    /** Força o reenvio imediato, ignorando o backoff — usado pelo botão manual na tela de admin. */
    @Transactional
    public EmailOutboxResponse resendNow(Long id) {
        EmailOutboxEntry entry = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de e-mail não encontrado: " + id));
        attempt(entry);
        return EmailOutboxResponse.fromEntity(entry);
    }

    /** Tenta enviar e grava o resultado na entidade. Nunca lança — quem precisa saber o
     * resultado confere o retorno ou o status salvo na entidade. */
    private boolean attempt(EmailOutboxEntry entry) {
        try {
            emailGateway.send(entry.getRecipientEmail(), entry.getSubject(), entry.getHtmlContent(), entry.getReplyTo());
            entry.markSent();
            repository.save(entry);
            return true;
        } catch (Exception e) {
            entry.recordFailure(e.getMessage());
            repository.save(entry);
            return false;
        }
    }

    /**
     * Roda com frequência (padrão: a cada 5 min) só para VERIFICAR se algo está pronto para
     * nova tentativa — cada e-mail individual só é retentado quando seu próprio
     * {@code nextRetryAt} vence, que cresce a cada falha (backoff). Não é "tenta de novo a cada
     * 5 minutos por 24h direto" — é um punhado de tentativas espaçadas ao longo de até 24h.
     */
    @Scheduled(fixedDelayString = "${app.email-outbox.retry-check-interval-ms:300000}")
    public void retryDuePending() {
        List<EmailOutboxEntry> due = repository.findByStatusAndNextRetryAtBefore(
                EmailOutboxStatus.FAILED, Instant.now());

        for (EmailOutboxEntry entry : due) {
            attempt(entry);
        }

        if (!due.isEmpty()) {
            log.info("Fila de e-mail: {} tentativa(s) de retry automático processada(s)", due.size());
        }
    }

    /**
     * Limpeza diária: e-mails entregues não precisam ficar guardados além de uma janela curta
     * (o registro permanente já existe em tb_audit_log); dead-letter fica mais tempo pra dar
     * chance de um admin perceber e agir, mas também não para sempre — minimização de dado
     * pessoal (LGPD), não só economia de espaço.
     */
    @Scheduled(cron = "${app.email-outbox.cleanup-cron:0 0 3 * * *}")
    public void cleanup() {
        int sentDeleted = repository.deleteByStatusAndUpdatedAtBefore(
                EmailOutboxStatus.SENT, Instant.now().minus(sentRetentionDays, ChronoUnit.DAYS));
        int deadLetterDeleted = repository.deleteByStatusAndUpdatedAtBefore(
                EmailOutboxStatus.DEAD_LETTER, Instant.now().minus(deadLetterRetentionDays, ChronoUnit.DAYS));

        if (sentDeleted > 0 || deadLetterDeleted > 0) {
            log.info("Limpeza da fila de e-mail: {} enviado(s) e {} dead-letter removido(s)",
                    sentDeleted, deadLetterDeleted);
        }
    }
}
