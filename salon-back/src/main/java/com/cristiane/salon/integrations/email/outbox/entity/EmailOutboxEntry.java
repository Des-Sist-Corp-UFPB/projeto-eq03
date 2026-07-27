package com.cristiane.salon.integrations.email.outbox.entity;

import com.cristiane.salon.integrations.email.outbox.enums.EmailOutboxStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Fila de retry para e-mails que falharam ao enviar, com um histórico curto (ver política de
 * limpeza em {@code EmailOutboxService}) de envios recentes para a tela de admin. Não é o
 * registro permanente de auditoria — isso continua sendo {@code tb_audit_log} (ação
 * {@code EMAIL_SENT}), que não tem prazo de retenção.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_email_outbox")
public class EmailOutboxEntry {

    /** Backoff crescente entre tentativas de retry automático (minutos). */
    public static final int[] BACKOFF_MINUTES = {5, 30, 120, 360, 1440};
    public static final int MAX_ATTEMPTS = BACKOFF_MINUTES.length;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "html_content", nullable = false, columnDefinition = "TEXT")
    private String htmlContent;

    @Column(name = "reply_to")
    private String replyTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailOutboxStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static EmailOutboxEntry create(String recipientEmail, String subject, String htmlContent,
            String replyTo, String relatedEntityType, Long relatedEntityId) {
        EmailOutboxEntry entry = new EmailOutboxEntry();
        entry.recipientEmail = recipientEmail;
        entry.subject = subject;
        entry.htmlContent = htmlContent;
        entry.replyTo = replyTo;
        entry.relatedEntityType = relatedEntityType;
        entry.relatedEntityId = relatedEntityId;
        entry.status = EmailOutboxStatus.PENDING;
        entry.attempts = 0;
        return entry;
    }

    public void markSent() {
        this.status = EmailOutboxStatus.SENT;
        this.sentAt = Instant.now();
        this.nextRetryAt = null;
    }

    /**
     * Registra uma tentativa que falhou. Depois de {@link #MAX_ATTEMPTS}, desiste de vez
     * (DEAD_LETTER) em vez de tentar pra sempre — um e-mail com endereço inválido ou domínio
     * inexistente nunca vai funcionar, e insistir indefinidamente arrisca a reputação de envio
     * da conta no provedor.
     */
    public void recordFailure(String errorMessage) {
        this.attempts++;
        this.lastError = truncate(errorMessage);
        if (this.attempts >= MAX_ATTEMPTS) {
            this.status = EmailOutboxStatus.DEAD_LETTER;
            this.nextRetryAt = null;
        } else {
            this.status = EmailOutboxStatus.FAILED;
            this.nextRetryAt = Instant.now().plus(BACKOFF_MINUTES[this.attempts - 1], ChronoUnit.MINUTES);
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
