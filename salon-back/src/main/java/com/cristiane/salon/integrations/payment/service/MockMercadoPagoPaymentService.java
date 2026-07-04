package com.cristiane.salon.integrations.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadopago.resources.payment.Payment;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@Profile("performance")
public class MockMercadoPagoPaymentService extends MercadoPagoPaymentService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Payment createPixPayment(BigDecimal amount, String description, String payerEmail, String payerName,
            String payerCpf, Long appointmentId) {
        try {
            String json = "{"
                    + "\"id\":123456789,"
                    + "\"status\":\"approved\","
                    + "\"external_reference\":\"" + appointmentId + "\","
                    + "\"transaction_amount\":" + amount + ","
                    + "\"point_of_interaction\":{"
                    + "  \"transaction_data\":{"
                    + "    \"qr_code\":\"mock_copia_e_cola_pix_code_123456789\""
                    + "  }"
                    + "}"
                    + "}";
            return objectMapper.readValue(json, Payment.class);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar mock de pagamento", e);
        }
    }

    @Override
    public Payment getPayment(Long paymentId) {
        try {
            String json = "{"
                    + "\"id\":" + paymentId + ","
                    + "\"status\":\"approved\","
                    + "\"external_reference\":\"1\","
                    + "\"transaction_amount\":100.00"
                    + "}";
            return objectMapper.readValue(json, Payment.class);
        } catch (Exception e) {
            return null;
        }
    }
}
