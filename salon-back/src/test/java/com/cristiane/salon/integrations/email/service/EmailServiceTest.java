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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private FeatureFlagService featureFlagService;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EmailService emailService;

    private Appointment appointment;
    private User client;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(emailService, "fromEmail", "notificacoes@elksandro.com");
        ReflectionTestUtils.setField(emailService, "businessEmail", "elksandrosandro19@gmail.com");
        ReflectionTestUtils.setField(emailService, "apiUrl", "http://test-email-api.com");

        client = new User();
        client.setId(10L);
        client.setEmail("client@example.com");

        appointment = new Appointment();
        appointment.setId(1L);
        appointment.setClient(client);
    }

    private void setupRestClientMock(MockedStatic<RestClient> mockedRestClient, boolean shouldFail) {
        RestClient restClient = mock(RestClient.class);
        mockedRestClient.when(() -> RestClient.create(anyString())).thenReturn(restClient);

        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        lenient().when(restClient.post()).thenReturn(requestBodyUriSpec);

        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);

        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        if (shouldFail) {
            lenient().when(responseSpec.toBodilessEntity()).thenThrow(new RuntimeException("API Connection Error"));
        } else {
            lenient().when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());
        }
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

        try (MockedStatic<RestClient> mockedRestClient = mockStatic(RestClient.class)) {
            setupRestClientMock(mockedRestClient, false);

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

        try (MockedStatic<RestClient> mockedRestClient = mockStatic(RestClient.class)) {
            setupRestClientMock(mockedRestClient, true);

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

        try (MockedStatic<RestClient> mockedRestClient = mockStatic(RestClient.class)) {
            setupRestClientMock(mockedRestClient, false);

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

        try (MockedStatic<RestClient> mockedRestClient = mockStatic(RestClient.class)) {
            setupRestClientMock(mockedRestClient, false);

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
    }

    @Test
    void sendCancellationNotification_whenClientAndStaffSucceed_shouldAuditSuccessForBoth() {
        // Arrange
        when(featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")).thenReturn(true);
        when(templateEngine.process(eq("mail/appointment-cancellation"), any(Context.class))).thenReturn("<html>Cancellation HTML</html>");

        try (MockedStatic<RestClient> mockedRestClient = mockStatic(RestClient.class)) {
            setupRestClientMock(mockedRestClient, false);

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
}
