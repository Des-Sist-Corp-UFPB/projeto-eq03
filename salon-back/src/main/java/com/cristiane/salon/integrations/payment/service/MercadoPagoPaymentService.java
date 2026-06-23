package com.cristiane.salon.integrations.payment.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import com.cristiane.salon.exception.BadRequestException;
import com.mercadopago.client.common.IdentificationRequest;
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
                            .firstName("Comprador")
                            .lastName("de Teste")
                            .identification(IdentificationRequest.builder()
                                    .type("CPF")
                                    .number("12345678909")
                                    .build())
                            .build())
                    .build();

            Payment payment = client.create(request);

            log.info("PIX gerado no Mercado Pago com sucesso para o Agendamento ID: {}", appointmentId);
            return payment;
        } catch (com.mercadopago.exceptions.MPApiException e) {
            // Essa é a linha de mestre: ela pega o JSON exato que o Mercado Pago devolveu com o motivo da recusa
            log.error("Mercado Pago recusou o pagamento! Status: {} | Detalhes: {}", 
                      e.getApiResponse().getStatusCode(), 
                      e.getApiResponse().getContent());
                      
            throw new BadRequestException("Falha ao gerar o PIX no Mercado Pago. Tente novamente mais tarde.");
        } catch (Exception e) {
            log.error("Erro ao comunicar com a API do Mercado Pago: ", e);
            throw new BadRequestException("Falha ao gerar o PIX no Mercado Pago. Tente novamente mais tarde.");
        }
    }

    public Payment getPayment(Long paymentId) {
        try {
            PaymentClient client = new PaymentClient();
            // Vai na API do Mercado Pago oficial consultar o status real desse ID
            return client.get(paymentId);
        } catch (Exception e) {
            log.error("Erro ao buscar pagamento no Mercado Pago: ", e);
            return null; // Se der erro (ex: ID falso de hacker), retorna nulo
        }
    }
}
