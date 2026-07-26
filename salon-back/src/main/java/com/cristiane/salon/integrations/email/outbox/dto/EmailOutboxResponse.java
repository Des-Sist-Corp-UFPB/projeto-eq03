package com.cristiane.salon.integrations.email.outbox.dto;

import com.cristiane.salon.integrations.email.outbox.entity.EmailOutboxEntry;
import com.cristiane.salon.integrations.email.outbox.enums.EmailOutboxStatus;

import java.time.LocalDateTime;

/**
 * Sem {@code htmlContent}: a tela de admin precisa saber PARA QUEM/O QUÊ/status, não o corpo
 * renderizado inteiro do e-mail — minimização de dado (LGPD), e evita listas paginadas pesadas.
 */
public record EmailOutboxResponse(
        Long id,
        String recipientEmail,
        String subject,
        EmailOutboxStatus status,
        int attempts,
        LocalDateTime nextRetryAt,
        String lastError,
        String relatedEntityType,
        Long relatedEntityId,
        LocalDateTime createdAt,
        LocalDateTime sentAt
) {
    public static EmailOutboxResponse fromEntity(EmailOutboxEntry entry) {
        return new EmailOutboxResponse(
                entry.getId(),
                entry.getRecipientEmail(),
                entry.getSubject(),
                entry.getStatus(),
                entry.getAttempts(),
                entry.getNextRetryAt(),
                entry.getLastError(),
                entry.getRelatedEntityType(),
                entry.getRelatedEntityId(),
                entry.getCreatedAt(),
                entry.getSentAt()
        );
    }
}
