package com.cristiane.salon.integrations.email.outbox.service;

import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.integrations.email.outbox.entity.EmailOutboxEntry;
import com.cristiane.salon.integrations.email.outbox.enums.EmailOutboxStatus;
import com.cristiane.salon.integrations.email.outbox.repository.EmailOutboxRepository;
import com.cristiane.salon.integrations.email.service.EmailGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class EmailOutboxServiceTest {

    @Mock
    private EmailOutboxRepository repository;

    @Mock
    private EmailGateway emailGateway;

    private EmailOutboxService service;

    @BeforeEach
    void setUp() {
        service = new EmailOutboxService(repository, emailGateway);
        ReflectionTestUtils.setField(service, "sentRetentionDays", 7);
        ReflectionTestUtils.setField(service, "deadLetterRetentionDays", 90);
    }

    @Test
    void sendNow_whenGatewaySucceeds_savesEntryAsSentAndDoesNotThrow() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.sendNow("cliente@example.com", "Assunto", "<p>Html</p>", "reply@example.com", "Appointment", 1L);

        ArgumentCaptor<EmailOutboxEntry> captor = ArgumentCaptor.forClass(EmailOutboxEntry.class);
        verify(repository, times(2)).save(captor.capture());
        EmailOutboxEntry saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(EmailOutboxStatus.SENT);
        verify(emailGateway).send("cliente@example.com", "Assunto", "<p>Html</p>", "reply@example.com");
    }

    @Test
    void sendNow_whenGatewayFails_savesEntryAsFailedAndPropagatesException() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("Resend fora do ar")).when(emailGateway)
                .send(any(), any(), any(), any());

        assertThatThrownBy(() -> service.sendNow("a@b.com", "s", "h", null, "Appointment", 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Resend fora do ar");

        ArgumentCaptor<EmailOutboxEntry> captor = ArgumentCaptor.forClass(EmailOutboxEntry.class);
        verify(repository, times(2)).save(captor.capture());
        EmailOutboxEntry saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(EmailOutboxStatus.FAILED);
        assertThat(saved.getAttempts()).isEqualTo(1);
    }

    @Test
    void resendNow_whenEntryExists_retriesImmediatelyIgnoringBackoff() {
        EmailOutboxEntry entry = EmailOutboxEntry.create("a@b.com", "s", "h", null, "Appointment", 1L);
        entry.recordFailure("timeout anterior");
        when(repository.findById(5L)).thenReturn(Optional.of(entry));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.resendNow(5L);

        verify(emailGateway).send("a@b.com", "s", "h", null);
        assertThat(entry.getStatus()).isEqualTo(EmailOutboxStatus.SENT);
    }

    @Test
    void resendNow_whenEntryDoesNotExist_throwsResourceNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resendNow(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void retryDuePending_attemptsEachDueEntryAndNeverThrows() {
        EmailOutboxEntry due1 = EmailOutboxEntry.create("a@b.com", "s", "h", null, null, null);
        EmailOutboxEntry due2 = EmailOutboxEntry.create("c@d.com", "s", "h", null, null, null);
        when(repository.findByStatusAndNextRetryAtBefore(eq(EmailOutboxStatus.FAILED), any()))
                .thenReturn(List.of(due1, due2));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("ainda fora do ar")).when(emailGateway)
                .send(any(), any(), any(), any());

        service.retryDuePending();

        verify(emailGateway, times(2)).send(any(), any(), any(), any());
        assertThat(due1.getStatus()).isEqualTo(EmailOutboxStatus.FAILED);
        assertThat(due2.getStatus()).isEqualTo(EmailOutboxStatus.FAILED);
    }

    @Test
    void cleanup_deletesSentOlderThanRetentionAndDeadLetterOlderThanRetention() {
        service.cleanup();

        verify(repository).deleteByStatusAndUpdatedAtBefore(eq(EmailOutboxStatus.SENT), any(Instant.class));
        verify(repository).deleteByStatusAndUpdatedAtBefore(eq(EmailOutboxStatus.DEAD_LETTER), any(Instant.class));
    }
}
