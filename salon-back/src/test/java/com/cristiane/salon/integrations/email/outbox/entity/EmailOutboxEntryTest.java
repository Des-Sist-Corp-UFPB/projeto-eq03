package com.cristiane.salon.integrations.email.outbox.entity;

import com.cristiane.salon.integrations.email.outbox.enums.EmailOutboxStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EmailOutboxEntryTest {

    @Test
    void create_startsAsPendingWithZeroAttempts() {
        EmailOutboxEntry entry = EmailOutboxEntry.create(
                "cliente@example.com", "Assunto", "<p>Html</p>", "reply@example.com", "Appointment", 1L);

        assertThat(entry.getStatus()).isEqualTo(EmailOutboxStatus.PENDING);
        assertThat(entry.getAttempts()).isZero();
        assertThat(entry.getRecipientEmail()).isEqualTo("cliente@example.com");
        assertThat(entry.getRelatedEntityType()).isEqualTo("Appointment");
        assertThat(entry.getRelatedEntityId()).isEqualTo(1L);
    }

    @Test
    void markSent_setsStatusAndSentAtAndClearsNextRetry() {
        EmailOutboxEntry entry = EmailOutboxEntry.create("a@b.com", "s", "h", null, null, null);
        entry.setNextRetryAt(LocalDateTime.now().plusMinutes(5));

        entry.markSent();

        assertThat(entry.getStatus()).isEqualTo(EmailOutboxStatus.SENT);
        assertThat(entry.getSentAt()).isNotNull();
        assertThat(entry.getNextRetryAt()).isNull();
    }

    @Test
    void recordFailure_beforeMaxAttempts_schedulesNextRetryWithBackoff() {
        EmailOutboxEntry entry = EmailOutboxEntry.create("a@b.com", "s", "h", null, null, null);

        entry.recordFailure("timeout");

        assertThat(entry.getStatus()).isEqualTo(EmailOutboxStatus.FAILED);
        assertThat(entry.getAttempts()).isEqualTo(1);
        assertThat(entry.getLastError()).isEqualTo("timeout");
        assertThat(entry.getNextRetryAt())
                .isAfter(LocalDateTime.now().plusMinutes(4))
                .isBefore(LocalDateTime.now().plusMinutes(6));
    }

    @Test
    void recordFailure_afterMaxAttempts_becomesDeadLetterWithNoNextRetry() {
        EmailOutboxEntry entry = EmailOutboxEntry.create("a@b.com", "s", "h", null, null, null);

        for (int i = 0; i < EmailOutboxEntry.MAX_ATTEMPTS; i++) {
            entry.recordFailure("falha " + i);
        }

        assertThat(entry.getStatus()).isEqualTo(EmailOutboxStatus.DEAD_LETTER);
        assertThat(entry.getAttempts()).isEqualTo(EmailOutboxEntry.MAX_ATTEMPTS);
        assertThat(entry.getNextRetryAt()).isNull();
    }

    @Test
    void recordFailure_truncatesVeryLongErrorMessages() {
        EmailOutboxEntry entry = EmailOutboxEntry.create("a@b.com", "s", "h", null, null, null);
        String longError = "x".repeat(600);

        entry.recordFailure(longError);

        assertThat(entry.getLastError()).hasSize(500);
    }
}
