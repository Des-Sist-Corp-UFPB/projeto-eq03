package com.cristiane.salon.integrations.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import com.cristiane.salon.exception.BadRequestException;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.resources.payment.Payment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MercadoPagoPaymentService {
    public Payment createPixPayment(BigDecimal amount, String description, String payerEmail, Long appointmentId) {
        try {
            PaymentClient client = new PaymentClient();

            PaymentCreateRequest request = PaymentCreateRequest.builder()
                    .transactionAmount(amount)
                    .description(description)
                    .paymentMethodId("pix")
                    .externalReference(appointmentId.toString())
                    .dateOfExpiration(OffsetDateTime.now().plusHours(24))
                    .payer(PaymentPayerRequest.builder()
                            .email(payerEmail)
                            .build())
                    .build();

            Payment payment = client.create(request);

            log.info("PIX gerado no Mercado Pago com sucesso para o Agendamento ID: {}", appointmentId);
            return payment;
        } catch (Exception e) {
            log.error("Erro ao comunicar com a API do Mercado Pago: ", e);
            throw new BadRequestException("Falha ao gerar o PIX no Mercado Pago. Tente novamente mais tarde.");
        }
    }
}
