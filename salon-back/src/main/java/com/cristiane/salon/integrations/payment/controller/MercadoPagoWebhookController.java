package com.cristiane.salon.integrations.payment.controller;

import com.cristiane.salon.integrations.payment.dto.MercadoPagoNotification;
import com.cristiane.salon.integrations.payment.service.MercadoPagoPaymentService;
import com.cristiane.salon.models.appointment.service.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/webhooks/mercadopago")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Endpoints para recebimento de notificações externas")
public class MercadoPagoWebhookController {

    private final AppointmentService appointmentService;
    private final MercadoPagoPaymentService mercadoPagoPaymentService;
    private final ObjectMapper objectMapper;

    @PostMapping
    @Operation(summary = "Recebe atualizações de pagamento do Mercado Pago")
    public ResponseEntity<Void> receiveNotification(
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody String rawBody) {

        log.info("Webhook recebido. x-signature: {}, x-request-id: {}, data.id: {}",
                xSignature != null ? "OK" : "FALTA",
                xRequestId != null ? "OK" : "FALTA",
                dataId);

        if (!mercadoPagoPaymentService.isValidSignature(xSignature, xRequestId, dataId)) {
            log.warn("Assinatura do webhook inválida ou ausente. Bloqueando requisição.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            MercadoPagoNotification notification = objectMapper.readValue(rawBody, MercadoPagoNotification.class);

            if (notification != null && "payment".equals(notification.type())
                    && notification.data() != null && notification.data().id() != null) {

                Long paymentId = Long.valueOf(notification.data().id());
                appointmentService.processPixPaymentWebhook(paymentId);
            }
        } catch (Exception e) {
            log.error("Erro ao processar/parsear webhook do Mercado Pago", e);
        }

        return ResponseEntity.ok().build();
    }
}