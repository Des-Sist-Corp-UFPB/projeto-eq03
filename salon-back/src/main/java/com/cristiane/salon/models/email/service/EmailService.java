package com.cristiane.salon.models.email.service;

import com.cristiane.salon.models.appointment.entity.Appointment;
import com.cristiane.salon.models.audit.AuditLogService;
import com.cristiane.salon.models.featureflag.service.FeatureFlagService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final FeatureFlagService featureFlagService;
    private final TemplateEngine templateEngine;
    private final AuditLogService auditLogService;

    @Value("${mail.from:notificacoes@elksandro.com}")
    private String fromEmail;

    @Value("${mail.business:elksandrosandro19@gmail.com}")
    private String businessEmail;

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

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("Cristiane Salon <" + fromEmail + ">");
            helper.setReplyTo(businessEmail);
            helper.setTo(businessEmail); // Notificação vai para o e-mail real do salão
            helper.setSubject("Novo Pedido de Agendamento Recebido");
            helper.setText(htmlContent, true);

            mailSender.send(message);
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

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("Cristiane Salon <" + fromEmail + ">");
            helper.setReplyTo(businessEmail); // Se o cliente responder, vai para a Cristiane
            helper.setTo(clientEmail);
            helper.setSubject("Seu Agendamento foi Confirmado!");
            helper.setText(htmlContent, true);

            mailSender.send(message);
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

    //@Async
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
                String htmlContent = templateEngine.process("mail/appointment-cancellation", context);

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom("Cristiane Salon <" + fromEmail + ">");
                helper.setReplyTo(businessEmail);
                helper.setTo(clientEmail);
                helper.setSubject("Agendamento Cancelado");
                helper.setText(htmlContent, true);

                mailSender.send(message);
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
            String htmlContent = templateEngine.process("mail/appointment-cancellation", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("Cristiane Salon <" + fromEmail + ">");
            helper.setReplyTo(businessEmail);
            helper.setTo(businessEmail); // Notificação interna para e-mail real
            helper.setSubject("Agendamento Cancelado");
            helper.setText(htmlContent, true);

            mailSender.send(message);
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
