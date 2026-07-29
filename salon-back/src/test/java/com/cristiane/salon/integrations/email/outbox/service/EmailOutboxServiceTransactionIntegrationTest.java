package com.cristiane.salon.integrations.email.outbox.service;

import com.cristiane.salon.integrations.email.outbox.entity.EmailOutboxEntry;
import com.cristiane.salon.integrations.email.outbox.enums.EmailOutboxStatus;
import com.cristiane.salon.integrations.email.outbox.repository.EmailOutboxRepository;
import com.cristiane.salon.integrations.email.service.EmailGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

/**
 * Regressão de um bug real de produção: {@code sendNow} é {@code @Transactional} e relança
 * exceção quando o envio falha, de propósito, pra quem chama (EmailService) continuar
 * decidindo o que fazer — mas o rollback automático do Spring pra RuntimeException desfazia
 * também o {@code repository.save(entry)} que acabou de gravar a falha, apagando no mesmo
 * instante o próprio registro que a fila existe pra preservar. Só um teste com transação real
 * (Spring context de verdade, não Mockito) consegue flagrar esse tipo de bug — é por isso que
 * ele passou despercebido em {@link EmailOutboxServiceTest}, que mocka o repositório e nunca
 * exercita rollback nenhum.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class EmailOutboxServiceTransactionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private EmailOutboxService service;

    @Autowired
    private EmailOutboxRepository repository;

    @MockitoBean
    private EmailGateway emailGateway;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void sendNow_whenGatewayFails_persistsTheFailedEntryDespiteTheRethrownException() {
        Mockito.doThrow(new RuntimeException("Credencial inválida"))
                .when(emailGateway).send(any(), any(), any(), any());

        assertThatThrownBy(() ->
                service.sendNow("cliente@example.com", "Assunto", "<p>Html</p>", null, "Appointment", 7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Credencial inválida");

        List<EmailOutboxEntry> all = repository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getStatus()).isEqualTo(EmailOutboxStatus.FAILED);
        assertThat(all.get(0).getAttempts()).isEqualTo(1);
        assertThat(all.get(0).getNextRetryAt()).isNotNull();
    }
}
