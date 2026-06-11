package com.cristiane.salon.models.email.service;

import com.cristiane.salon.models.appointment.entity.Appointment;
import com.cristiane.salon.models.audit.AuditLogService;
import com.cristiane.salon.models.featureflag.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final FeatureFlagService featureFlagService;
    private final TemplateEngine templateEngine;
    private final AuditLogService auditLogService;

    @Value("${mail.password}")
    private String apiKey;

    @Value("${mail.from:notificacoes@elksandro.com}")
    private String fromEmail;

    @Value("${mail.business:elksandrosandro19@gmail.com}")
    private String businessEmail;

    @Value("${mail.api-url}")
    private String apiUrl;

    private void sendViaHttpApi(String to, String subject, String htmlContent, String replyTo) {
        RestClient restClient = RestClient.create(apiUrl);

        Map<String, Object> payload = Map.of(
                "from", "Cristiane Salon <" + fromEmail + ">",
                "to", new String[]{to},
                "subject", subject,
                "html", htmlContent,
                "reply_to", replyTo
        );

        restClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
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
            String htmlContent = templateEngine.process("mail/appointment-request", context);

            sendViaHttpApi(businessEmail, "Novo Pedido de Agendamento Recebido", htmlContent, businessEmail);
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

            sendViaHttpApi(clientEmail, "Seu Agendamento foi Confirmado!", htmlContent, businessEmail);
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

                sendViaHttpApi(clientEmail, "Agendamento Cancelado", htmlContent, businessEmail);
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

            sendViaHttpApi(businessEmail, "Agendamento Cancelado", htmlContent, businessEmail);
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
}
