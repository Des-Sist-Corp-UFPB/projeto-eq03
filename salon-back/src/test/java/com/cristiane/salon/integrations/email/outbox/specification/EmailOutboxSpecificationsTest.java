package com.cristiane.salon.integrations.email.outbox.specification;

import com.cristiane.salon.integrations.email.outbox.dto.EmailOutboxFilter;
import com.cristiane.salon.integrations.email.outbox.entity.EmailOutboxEntry;
import com.cristiane.salon.integrations.email.outbox.enums.EmailOutboxStatus;
import com.cristiane.salon.integrations.email.outbox.repository.EmailOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class EmailOutboxSpecificationsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private EmailOutboxRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        EmailOutboxEntry sent = EmailOutboxEntry.create("sent@example.com", "s", "h", null, "Appointment", 1L);
        sent.markSent();
        repository.save(sent);

        EmailOutboxEntry failed = EmailOutboxEntry.create("failed@example.com", "s", "h", null, "Appointment", 2L);
        failed.recordFailure("timeout");
        repository.save(failed);

        EmailOutboxEntry deadLetter = EmailOutboxEntry.create("dead@example.com", "s", "h", null, "Appointment", 3L);
        for (int i = 0; i < EmailOutboxEntry.MAX_ATTEMPTS; i++) {
            deadLetter.recordFailure("timeout");
        }
        repository.save(deadLetter);
    }

    @Test
    void filter_withNoStatuses_returnsAll() {
        Page<EmailOutboxEntry> result = repository.findAll(
                EmailOutboxSpecifications.filter(new EmailOutboxFilter(null)), PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void filter_withSentStatus_returnsOnlySent() {
        Page<EmailOutboxEntry> result = repository.findAll(
                EmailOutboxSpecifications.filter(new EmailOutboxFilter(List.of(EmailOutboxStatus.SENT))),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRecipientEmail()).isEqualTo("sent@example.com");
    }

    @Test
    void filter_withFailedAndDeadLetterStatuses_returnsBoth() {
        Page<EmailOutboxEntry> result = repository.findAll(
                EmailOutboxSpecifications.filter(new EmailOutboxFilter(
                        List.of(EmailOutboxStatus.FAILED, EmailOutboxStatus.DEAD_LETTER))),
                PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(EmailOutboxEntry::getRecipientEmail)
                .containsExactlyInAnyOrder("failed@example.com", "dead@example.com");
    }
}
