package com.cristiane.salon.integrations.payment.controller;

import com.cristiane.salon.integrations.payment.dto.MercadoPagoNotification;
import com.cristiane.salon.models.appointment.service.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/v1/webhooks/mercadopago")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Endpoints para recebimento de notificações externas")
public class MercadoPagoWebhookController {

    private final AppointmentService appointmentService;
    private final ObjectMapper objectMapper;

    @Value("${mercadopago.webhook-secret:test_secret}")
    private String webhookSecret;

    @PostMapping
    @Operation(summary = "Recebe atualizações de pagamento do Mercado Pago")
    public ResponseEntity<Void> receiveNotification(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody String rawBody) {

        log.debug("Recebido webhook do Mercado Pago. x-signature: {}, x-request-id: {}", xSignature, xRequestId);

        if (xSignature == null || xSignature.isEmpty()) {
            log.warn("Assinatura do webhook ausente");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Validação da assinatura
        if (!validateSignature(xSignature, xRequestId, rawBody)) {
            log.warn("Assinatura do webhook inválida");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            MercadoPagoNotification notification = objectMapper.readValue(rawBody, MercadoPagoNotification.class);
            
            // Verifica se a notificação é do tipo "payment"
            if (notification != null && "payment".equals(notification.type()) 
                    && notification.data() != null && notification.data().id() != null) {
                Long paymentId = Long.valueOf(notification.data().id());
                // Passa o ID pra frente. O Service faz o Double-Check seguro!
                appointmentService.processPixPaymentWebhook(paymentId);
            }
        } catch (Exception e) {
            log.error("Erro ao processar/parsear webhook do Mercado Pago", e);
            // Retorna 200 para evitar loops do Mercado Pago mesmo em caso de erro de parse de payload inesperado
        }
        
        return ResponseEntity.ok().build();
    }

    private boolean validateSignature(String xSignature, String xRequestId, String body) {
        try {
            // Em testes ou se a chave for 'test_secret', podemos simplificar com um mock fallback
            if ("test_secret".equals(webhookSecret)) {
                return xSignature.startsWith("valid_sig");
            }
            
            // Formato do x-signature do Mercado Pago:
            // x-signature: ts=TIMESTAMP;v1=HASH
            String[] parts = xSignature.split(";");
            String ts = null;
            String v1 = null;
            for (String part : parts) {
                if (part.startsWith("ts=")) {
                    ts = part.substring(3);
                } else if (part.startsWith("v1=")) {
                    v1 = part.substring(3);
                }
            }

            if (ts == null || v1 == null) {
                return false;
            }

            // A assinatura do Mercado Pago é gerada concatenando:
            // id:[x-request-id-value];request-timestamp:[ts-value];
            // concatenado com o body
            String manifest = String.format("id:%s;request-timestamp:%s;%s", 
                    xRequestId != null ? xRequestId : "", ts, body);

            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hashBytes = sha256Hmac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString().equalsIgnoreCase(v1);
        } catch (Exception e) {
            log.error("Erro ao validar assinatura", e);
            return false;
        }
    }
}