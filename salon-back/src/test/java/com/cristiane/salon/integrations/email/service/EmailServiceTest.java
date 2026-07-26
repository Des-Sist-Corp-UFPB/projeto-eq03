package com.cristiane.salon.integrations.email.service;

import com.cristiane.salon.models.appointment.entity.Appointment;
import com.cristiane.salon.models.audit.AuditLogService;
import com.cristiane.salon.models.featureflag.service.FeatureFlagService;
import com.cristiane.salon.models.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private FeatureFlagService featureFlagService;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private com.cristiane.salon.integrations.email.outbox.service.EmailOutboxService emailOutboxService;

    @InjectMocks
    private EmailService emailService;

    private Appointment appointment;
    private User client;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "businessEmail", "elksandrosandro19@gmail.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:5173");

        client = new User();
        client.setId(10L);
        client.setEmail("client@example.com");

        appointment = new Appointment();
        appointment.setId(1L);
        appointment.setClient(client);
    }

    private void setupGatewayMock(boolean shouldFail) {
        if (shouldFail) {
            doThrow(new RuntimeException("API Connection Error"))
                    .when(emailOutboxService).sendNow(any(), any(), any(), any(), any(), any());
        }
        // Sucesso: método void, não precisa de stub (não faz nada por padrão).
    }

    @Test
    void sendRequestNotificationToStaff_whenFeatureFlagDisabled_shouldReturnImmediately() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(false);

        // Act
        emailService.sendRequestNotificationToStaff(appointment);

        // Assert
        verifyNoInteractions(templateEngine, auditLogService);
    }

    @Test
    void sendRequestNotificationToStaff_whenSuccessful_shouldSendEmailAndAuditSuccess() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
        when(templateEngine.process(eq("mail/appointment-request"), any(Context.class))).thenReturn("<html>Request HTML</html>");

        setupGatewayMock(false);

        // Act
        emailService.sendRequestNotificationToStaff(appointment);

        // Assert
        verify(auditLogService).logAction(
                isNull(),
                eq("SYSTEM"),
                eq("EMAIL_SENT"),
                eq("Appointment"),
                eq(1L),
                eq("E-mail de solicitação de agendamento enviado para a equipe (elksandrosandro19@gmail.com)"),
                eq("SUCCESS")
        );
    }

    @Test
    void sendRequestNotificationToStaff_whenSuccessful_shouldPassFrontendUrlToTemplateContext() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
        org.mockito.ArgumentCaptor<Context> contextCaptor = org.mockito.ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(eq("mail/appointment-request"), contextCaptor.capture())).thenReturn("<html>Request HTML</html>");

        setupGatewayMock(false);

        // Act
        emailService.sendRequestNotificationToStaff(appointment);

        // Assert: sem isso, o link "Visualizar no Painel" do e-mail apontaria pra
        // localhost mesmo em produção (bug real corrigido nesta mudança).
        assertThat(contextCaptor.getValue().getVariable("frontendUrl")).isEqualTo("http://localhost:5173");
    }

    @Test
    void sendRequestNotificationToStaff_whenTemplateProcessingThrowsException_shouldAuditFailure() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
        when(templateEngine.process(eq("mail/appointment-request"), any(Context.class)))
                .thenThrow(new RuntimeException("Thymeleaf parsing error"));

        // Act
        emailService.sendRequestNotificationToStaff(appointment);

        // Assert
        verify(auditLogService).logAction(
                isNull(),
                eq("SYSTEM"),
                eq("EMAIL_SENT"),
                eq("Appointment"),
                eq(1L),
                eq("Falha ao enviar e-mail de solicitação de agendamento para a equipe (elksandrosandro19@gmail.com)"),
                eq("FAILURE"),
                eq("Thymeleaf parsing error")
        );
    }

    @Test
    void sendRequestNotificationToStaff_whenApiThrowsException_shouldAuditFailure() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
        when(templateEngine.process(eq("mail/appointment-request"), any(Context.class))).thenReturn("<html>Request HTML</html>");

        setupGatewayMock(true);

        // Act
        emailService.sendRequestNotificationToStaff(appointment);

        // Assert
        verify(auditLogService).logAction(
                isNull(),
                eq("SYSTEM"),
                eq("EMAIL_SENT"),
                eq("Appointment"),
                eq(1L),
                eq("Falha ao enviar e-mail de solicitação de agendamento para a equipe (elksandrosandro19@gmail.com)"),
                eq("FAILURE"),
                eq("API Connection Error")
        );
    }

    // --- sendConfirmationNotificationToClient ---

    @Test
    void sendConfirmationNotificationToClient_whenFeatureFlagDisabled_shouldReturnImmediately() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(false);

        // Act
        emailService.sendConfirmationNotificationToClient(appointment);

        // Assert
        verifyNoInteractions(templateEngine);
    }

    @Test
    void sendConfirmationNotificationToClient_whenClientEmailNull_shouldReturnImmediately() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
        client.setEmail(null);

        // Act
        emailService.sendConfirmationNotificationToClient(appointment);

        // Assert
        verifyNoInteractions(templateEngine);
    }

    @Test
    void sendConfirmationNotificationToClient_whenClientEmailBlank_shouldReturnImmediately() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
        client.setEmail("   ");

        // Act
        emailService.sendConfirmationNotificationToClient(appointment);

        // Assert
        verifyNoInteractions(templateEngine);
    }

    @Test
    void sendConfirmationNotificationToClient_whenSuccessful_shouldSendEmailAndAuditSuccess() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
        when(templateEngine.process(eq("mail/appointment-confirmation"), any(Context.class))).thenReturn("<html>Confirmation HTML</html>");

        setupGatewayMock(false);

        // Act
        emailService.sendConfirmationNotificationToClient(appointment);

        // Assert
        verify(auditLogService).logAction(
                isNull(),
                eq("SYSTEM"),
                eq("EMAIL_SENT"),
                eq("Appointment"),
                eq(1L),
                eq("E-mail de confirmação de agendamento enviado para: client@example.com"),
                eq("SUCCESS")
        );
    }

    @Test
    void sendConfirmationNotificationToClient_whenThrowsException_shouldAuditFailure() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
        when(templateEngine.process(eq("mail/appointment-confirmation"), any(Context.class)))
                .thenThrow(new RuntimeException("Template error"));

        // Act
        emailService.sendConfirmationNotificationToClient(appointment);

        // Assert
        verify(auditLogService).logAction(
                isNull(),
                eq("SYSTEM"),
                eq("EMAIL_SENT"),
                eq("Appointment"),
                eq(1L),
                eq("Falha ao enviar e-mail de confirmação para: client@example.com"),
                eq("FAILURE"),
                eq("Template error")
        );
    }

    // --- sendCancellationNotification ---

    @Test
    void sendCancellationNotification_whenFeatureFlagDisabled_shouldReturnImmediately() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(false);

        // Act
        emailService.sendCancellationNotification(appointment);

        // Assert
        verifyNoInteractions(templateEngine);
    }

    @Test
    void sendCancellationNotification_whenClientEmailNull_shouldOnlyNotifyStaffAndAudit() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
        client.setEmail(null);
        when(templateEngine.process(eq("mail/appointment-cancellation"), any(Context.class))).thenReturn("<html>Cancellation HTML</html>");

        setupGatewayMock(false);

        // Act
        emailService.sendCancellationNotification(appointment);

        // Assert
        // Notified staff (success)
        verify(auditLogService).logAction(
                isNull(),
                eq("SYSTEM"),
                eq("EMAIL_SENT"),
                eq("Appointment"),
                eq(1L),
                eq("E-mail de cancelamento de agendamento enviado para a equipe (elksandrosandro19@gmail.com)"),
                eq("SUCCESS")
        );
        // No client log
        verify(auditLogService, never()).logAction(
                any(), any(), any(), any(), any(), contains("cliente"), any(), any()
        );
    }

    @Test
    void sendCancellationNotification_whenClientAndStaffSucceed_shouldAuditSuccessForBoth() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
        when(templateEngine.process(eq("mail/appointment-cancellation"), any(Context.class))).thenReturn("<html>Cancellation HTML</html>");

        setupGatewayMock(false);

        // Act
        emailService.sendCancellationNotification(appointment);

        // Assert
        verify(auditLogService).logAction(
                isNull(),
                eq("SYSTEM"),
                eq("EMAIL_SENT"),
                eq("Appointment"),
                eq(1L),
                eq("E-mail de cancelamento de agendamento enviado para o cliente: client@example.com"),
                eq("SUCCESS")
        );
        verify(auditLogService).logAction(
                isNull(),
                eq("SYSTEM"),
                eq("EMAIL_SENT"),
                eq("Appointment"),
                eq(1L),
                eq("E-mail de cancelamento de agendamento enviado para a equipe (elksandrosandro19@gmail.com)"),
                eq("SUCCESS")
        );
    }

    @Test
    void sendCancellationNotification_whenClientAndStaffFail_shouldAuditFailureForBoth() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
        when(templateEngine.process(eq("mail/appointment-cancellation"), any(Context.class)))
                .thenThrow(new RuntimeException("Template load error"));

        // Act
        emailService.sendCancellationNotification(appointment);

        // Assert
        verify(auditLogService).logAction(
                isNull(),
                eq("SYSTEM"),
                eq("EMAIL_SENT"),
                eq("Appointment"),
                eq(1L),
                eq("Falha ao enviar e-mail de cancelamento para o cliente: client@example.com"),
                eq("FAILURE"),
                eq("Template load error")
        );
        verify(auditLogService).logAction(
                isNull(),
                eq("SYSTEM"),
                eq("EMAIL_SENT"),
                eq("Appointment"),
                eq(1L),
                eq("Falha ao enviar e-mail de cancelamento para a equipe (elksandrosandro19@gmail.com)"),
                eq("FAILURE"),
                eq("Template load error")
        );
    }

    // --- sendPasswordResetEmail ---

    @Test
    void sendPasswordResetEmail_whenFeatureFlagDisabled_shouldReturnImmediately() {
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(false);

        emailService.sendPasswordResetEmail(client, "raw-token");

        verifyNoInteractions(templateEngine, auditLogService);
    }

    @Test
    void sendPasswordResetEmail_whenSuccessful_shouldBuildLinkWithFrontendUrlAndAuditSuccess() {
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
        org.mockito.ArgumentCaptor<Context> contextCaptor = org.mockito.ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(eq("mail/password-reset"), contextCaptor.capture())).thenReturn("<html>Reset HTML</html>");

        setupGatewayMock(false);

        emailService.sendPasswordResetEmail(client, "raw-token");

        assertThat(contextCaptor.getValue().getVariable("resetLink"))
                .isEqualTo("http://localhost:5173/reset-password?token=raw-token");

        verify(auditLogService).logAction(
                eq(10L),
                eq("SYSTEM"),
                eq("EMAIL_SENT"),
                eq("User"),
                eq(10L),
                eq("E-mail de redefinição de senha enviado para: client@example.com"),
                eq("SUCCESS")
        );
    }

    @Test
    void sendPasswordResetEmail_whenTemplateProcessingThrows_shouldAuditFailure() {
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
        when(templateEngine.process(eq("mail/password-reset"), any(Context.class)))
                .thenThrow(new RuntimeException("Thymeleaf parsing error"));

        emailService.sendPasswordResetEmail(client, "raw-token");

        verify(auditLogService).logAction(
                eq(10L),
                eq("SYSTEM"),
                eq("EMAIL_SENT"),
                eq("User"),
                eq(10L),
                eq("Falha ao enviar e-mail de redefinição de senha para: client@example.com"),
                eq("FAILURE"),
                eq("Thymeleaf parsing error")
        );
    }
}
