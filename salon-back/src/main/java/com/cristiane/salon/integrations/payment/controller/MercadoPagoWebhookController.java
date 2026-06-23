package com.cristiane.salon.integrations.payment.controller;

import com.cristiane.salon.integrations.payment.dto.MercadoPagoNotification;
import com.cristiane.salon.models.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/webhooks/mercadopago")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Endpoints para recebimento de notificações externas")
public class MercadoPagoWebhookController {

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Recebe atualizações de pagamento do Mercado Pago")
    public ResponseEntity<Void> receiveNotification(@RequestBody MercadoPagoNotification notification) {
        
        // Verifica se a notificação é do tipo "payment" (pois o MP manda avisos de outras coisas também)
        if ("payment".equals(notification.type()) && notification.data() != null && notification.data().id() != null) {
            try {
                Long paymentId = Long.valueOf(notification.data().id());
                // Passa o ID pra frente. O Service faz o Double-Check seguro!
                appointmentService.processPixPaymentWebhook(paymentId);
            } catch (Exception e) {
                log.error("Erro ao processar webhook do Mercado Pago", e);
            }
        }
        
        // Retorna HTTP 200 rápido pro Mercado Pago não ficar mandando a mesma mensagem em loop
        return ResponseEntity.ok().build();
    }
}