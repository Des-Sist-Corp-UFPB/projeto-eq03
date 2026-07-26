package com.cristiane.salon.integrations.email.service;

import com.cristiane.salon.integrations.email.outbox.service.EmailOutboxService;
import com.cristiane.salon.models.appointment.entity.Appointment;
import com.cristiane.salon.models.audit.AuditLogService;
import com.cristiane.salon.models.featureflag.service.FeatureFlagService;
import com.cristiane.salon.models.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import org.springframework.context.annotation.Profile;

@Profile("!performance")
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final FeatureFlagService featureFlagService;
    private final TemplateEngine templateEngine;
    private final AuditLogService auditLogService;
    private final EmailOutboxService emailOutboxService;

    @Value("${mail.business:elksandrosandro19@gmail.com}")
    private String businessEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // Grava na fila de retry (EmailOutboxService) além de tentar enviar na hora — se falhar,
    // a exceção propaga igual antes, e cada método continua registrando o FAILURE no log de
    // auditoria como já fazia; a diferença é que agora também fica registrado para reenvio
    // automático depois, em vez de ser perdido para sempre.
    private void sendViaHttpApi(String to, String subject, String htmlContent, String replyTo,
            String relatedEntityType, Long relatedEntityId) {
        emailOutboxService.sendNow(to, subject, htmlContent, replyTo, relatedEntityType, relatedEntityId);
    }

    @Async
    public void sendRequestNotificationToStaff(Appointment appointment) {
        if (!featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")) {
            log.info("Envio de e-mail desativado por Feature Flag (EMAIL_NOTIFICATIONS).");
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("appointment", appointment);
            context.setVariable("frontendUrl", frontendUrl);
            String htmlContent = templateEngine.process("mail/appointment-request", context);

            sendViaHttpApi(businessEmail, "Novo Pedido de Agendamento Recebido", htmlContent, businessEmail,
                    "Appointment", appointment.getId());
            log.info("E-mail de notificação de solicitação enviado com sucesso para a equipe.");

            auditLogService.logAction(
                    null,
                    "SYSTEM",
                    "EMAIL_SENT",
                    "Appointment",
                    appointment.getId(),
                    "E-mail de solicitação de agendamento enviado para a equipe (" + businessEmail + ")",
                    "SUCCESS");
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail de solicitação para a equipe: {}", e.getMessage());
            auditLogService.logAction(
                    null,
                    "SYSTEM",
                    "EMAIL_SENT",
                    "Appointment",
                    appointment.getId(),
                    "Falha ao enviar e-mail de solicitação de agendamento para a equipe (" + businessEmail + ")",
                    "FAILURE",
                    e.getMessage());
        }
    }

    @Async
    public void sendConfirmationNotificationToClient(Appointment appointment) {
        if (!featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")) {
            log.info("Envio de e-mail desativado por Feature Flag (EMAIL_NOTIFICATIONS).");
            return;
        }

        String clientEmail = appointment.getClient().getEmail();
        if (clientEmail == null || clientEmail.trim().isEmpty()) {
            log.info("Cliente não possui e-mail cadastrado.");
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("appointment", appointment);
            String htmlContent = templateEngine.process("mail/appointment-confirmation", context);

            sendViaHttpApi(clientEmail, "Seu Agendamento foi Confirmado!", htmlContent, businessEmail,
                    "Appointment", appointment.getId());
            log.info("E-mail de confirmação enviado com sucesso para: {}", clientEmail);

            auditLogService.logAction(
                    null,
                    "SYSTEM",
                    "EMAIL_SENT",
                    "Appointment",
                    appointment.getId(),
                    "E-mail de confirmação de agendamento enviado para: " + clientEmail,
                    "SUCCESS");
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail de confirmação para o cliente {}: {}", clientEmail, e.getMessage());
            auditLogService.logAction(
                    null,
                    "SYSTEM",
                    "EMAIL_SENT",
                    "Appointment",
                    appointment.getId(),
                    "Falha ao enviar e-mail de confirmação para: " + clientEmail,
                    "FAILURE",
                    e.getMessage());
        }
    }

    @Async
    public void sendPaymentConfirmationNotificationToClient(Appointment appointment) {
        if (!featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")) {
            log.info("Envio de e-mail desativado por Feature Flag (EMAIL_NOTIFICATIONS).");
            return;
        }

        String clientEmail = appointment.getClient().getEmail();
        if (clientEmail == null || clientEmail.trim().isEmpty()) {
            log.info("Cliente não possui e-mail cadastrado.");
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("appointment", appointment);
            String htmlContent = templateEngine.process("mail/payment-confirmation", context);

            sendViaHttpApi(clientEmail, "Pagamento Recebido e Confirmado!", htmlContent, businessEmail,
                    "Appointment", appointment.getId());
            log.info("E-mail de confirmação de pagamento enviado com sucesso para: {}", clientEmail);

            auditLogService.logAction(
                    null,
                    "SYSTEM",
                    "EMAIL_SENT",
                    "Appointment",
                    appointment.getId(),
                    "E-mail de confirmação de pagamento enviado para: " + clientEmail,
                    "SUCCESS");
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail de confirmação de pagamento para o cliente {}: {}", clientEmail, e.getMessage());
            auditLogService.logAction(
                    null,
                    "SYSTEM",
                    "EMAIL_SENT",
                    "Appointment",
                    appointment.getId(),
                    "Falha ao enviar e-mail de confirmação de pagamento para: " + clientEmail,
                    "FAILURE",
                    e.getMessage());
        }
    }

    @Async
    public void sendCancellationNotification(Appointment appointment) {
        if (!featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")) {
            log.info("Envio de e-mail desativado por Feature Flag (EMAIL_NOTIFICATIONS).");
            return;
        }

        // Notify client
        String clientEmail = appointment.getClient().getEmail();
        if (clientEmail != null && !clientEmail.trim().isEmpty()) {
            try {
                Context context = new Context();
                context.setVariable("appointment", appointment);
                context.setVariable("isStaff", false);
                String htmlContent = templateEngine.process("mail/appointment-cancellation", context);

                sendViaHttpApi(clientEmail, "Agendamento Cancelado", htmlContent, businessEmail,
                        "Appointment", appointment.getId());
                log.info("E-mail de cancelamento enviado com sucesso para o cliente: {}", clientEmail);

                auditLogService.logAction(
                        null,
                        "SYSTEM",
                        "EMAIL_SENT",
                        "Appointment",
                        appointment.getId(),
                        "E-mail de cancelamento de agendamento enviado para o cliente: " + clientEmail,
                        "SUCCESS");
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail de cancelamento para o cliente {}: {}", clientEmail, e.getMessage());
                auditLogService.logAction(
                        null,
                        "SYSTEM",
                        "EMAIL_SENT",
                        "Appointment",
                        appointment.getId(),
                        "Falha ao enviar e-mail de cancelamento para o cliente: " + clientEmail,
                        "FAILURE",
                        e.getMessage());
            }
        }

        // Notify staff/admin
        try {
            Context context = new Context();
            context.setVariable("appointment", appointment);
            context.setVariable("isStaff", true);
            String htmlContent = templateEngine.process("mail/appointment-cancellation", context);

            sendViaHttpApi(businessEmail, "Agendamento Cancelado", htmlContent, businessEmail,
                    "Appointment", appointment.getId());
            log.info("E-mail de cancelamento enviado com sucesso para a equipe.");

            auditLogService.logAction(
                    null,
                    "SYSTEM",
                    "EMAIL_SENT",
                    "Appointment",
                    appointment.getId(),
                    "E-mail de cancelamento de agendamento enviado para a equipe (" + businessEmail + ")",
                    "SUCCESS");
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail de cancelamento para a equipe: {}", e.getMessage());
            auditLogService.logAction(
                    null,
                    "SYSTEM",
                    "EMAIL_SENT",
                    "Appointment",
                    appointment.getId(),
                    "Falha ao enviar e-mail de cancelamento para a equipe (" + businessEmail + ")",
                    "FAILURE",
                    e.getMessage());
        }
    }

    @Async
    public void sendAppointmentReminder(Appointment appointment) {
        if (!featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")) {
            log.info("Envio de e-mail desativado por Feature Flag (EMAIL_NOTIFICATIONS).");
            return;
        }

        String clientEmail = appointment.getClient().getEmail();
        if (clientEmail == null || clientEmail.trim().isEmpty()) {
            log.info("Cliente não possui e-mail cadastrado.");
            return;
        }

        try {
            Context context = new Context();
            context.setVariable("appointment", appointment);
            context.setVariable("frontendUrl", frontendUrl);
            String htmlContent = templateEngine.process("mail/appointment-reminder", context);

            sendViaHttpApi(clientEmail, "Lembrete: seu agendamento é amanhã!", htmlContent, businessEmail,
                    "Appointment", appointment.getId());
            log.info("E-mail de lembrete de agendamento (D-1) enviado com sucesso para: {}", clientEmail);

            auditLogService.logAction(
                    null,
                    "SYSTEM",
                    "EMAIL_SENT",
                    "Appointment",
                    appointment.getId(),
                    "Lembrete de agendamento (D-1) enviado para: " + clientEmail,
                    "SUCCESS");
        } catch (Exception e) {
            log.warn("Falha ao enviar lembrete de agendamento (D-1) para o cliente {}: {}", clientEmail, e.getMessage());
            auditLogService.logAction(
                    null,
                    "SYSTEM",
                    "EMAIL_SENT",
                    "Appointment",
                    appointment.getId(),
                    "Falha ao enviar lembrete de agendamento (D-1) para: " + clientEmail,
                    "FAILURE",
                    e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(User user, String rawToken) {
        if (!featureFlagService.isEnabled("EMAIL_NOTIFICATIONS")) {
            log.info("Envio de e-mail desativado por Feature Flag (EMAIL_NOTIFICATIONS).");
            return;
        }

        try {
            String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
            Context context = new Context();
            context.setVariable("userName", user.getName());
            context.setVariable("resetLink", resetLink);
            String htmlContent = templateEngine.process("mail/password-reset", context);

            sendViaHttpApi(user.getEmail(), "Redefinição de Senha", htmlContent, businessEmail,
                    "User", user.getId());
            log.info("E-mail de redefinição de senha enviado com sucesso para: {}", user.getEmail());

            auditLogService.logAction(
                    user.getId(),
                    "SYSTEM",
                    "EMAIL_SENT",
                    "User",
                    user.getId(),
                    "E-mail de redefinição de senha enviado para: " + user.getEmail(),
                    "SUCCESS");
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail de redefinição de senha para {}: {}", user.getEmail(), e.getMessage());
            auditLogService.logAction(
                    user.getId(),
                    "SYSTEM",
                    "EMAIL_SENT",
                    "User",
                    user.getId(),
                    "Falha ao enviar e-mail de redefinição de senha para: " + user.getEmail(),
                    "FAILURE",
                    e.getMessage());
        }
    }
}
