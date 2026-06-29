package com.cristiane.salon.integrations.payment.service;

import com.cristiane.salon.exception.BadRequestException;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.net.HttpStatus;
import com.mercadopago.net.MPResponse;
import com.mercadopago.resources.payment.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MercadoPagoPaymentServiceTest {

    @InjectMocks
    private MercadoPagoPaymentService paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "test_secret");
    }

    @Test
    void isValidSignature_whenWebhookSecretIsTestSecret_shouldValidateWithValidPrefix() {
        assertThat(paymentService.isValidSignature("valid_sig_123", "req-1", "data-1")).isTrue();
        assertThat(paymentService.isValidSignature("invalid_sig", "req-1", "data-1")).isFalse();
    }

    @Test
    void isValidSignature_whenWebhookSecretIsReal_shouldCalculateAndValidate() {
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "my_real_secret");

        // Validate basic parsing errors & invalid signature checks
        assertThat(paymentService.isValidSignature(null, "2", "1")).isFalse();
        assertThat(paymentService.isValidSignature("", "2", "1")).isFalse();
        assertThat(paymentService.isValidSignature("ts=12345", "2", "1")).isFalse(); // no v1
        assertThat(paymentService.isValidSignature("v1=abc", "2", "1")).isFalse(); // no ts
    }

    @Test
    void isValidSignature_whenWebhookSecretIsRealAndSignatureIsValid_shouldReturnTrue() throws Exception {
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "my_real_secret");

        String ts = "123456789";
        String dataId = "987654321";
        String xRequestId = "req-123";
        
        String manifest = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";
        
        javax.crypto.Mac sha256Hmac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                "my_real_secret".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"
        );
        sha256Hmac.init(secretKey);
        byte[] hashBytes = sha256Hmac.doFinal(manifest.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        String calculatedV1 = hexString.toString();
        String xSignature = "ts=" + ts + ",v1=" + calculatedV1;

        boolean result = paymentService.isValidSignature(xSignature, xRequestId, dataId);
        assertThat(result).isTrue();
    }

    @Test
    void isValidSignature_whenWebhookSecretIsRealAndSignatureIsInvalid_shouldReturnFalse() throws Exception {
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "my_real_secret");

        String ts = "123456789";
        String dataId = "987654321";
        String xRequestId = "req-123";
        String xSignature = "ts=" + ts + ",v1=wronghash";

        boolean result = paymentService.isValidSignature(xSignature, xRequestId, dataId);
        assertThat(result).isFalse();
    }

    @Test
    void createPixPayment_whenSuccessful_shouldReturnPayment() throws Exception {
        Payment mockPayment = mock(Payment.class);

        try (MockedConstruction<PaymentClient> mockedClient = mockConstruction(PaymentClient.class,
                (mock, context) -> when(mock.create(any())).thenReturn(mockPayment))) {

            Payment result = paymentService.createPixPayment(
                    BigDecimal.TEN, "Description", "payer@email.com",
                    "Payer Name", "12345678909", 1L
            );

            assertThat(result).isSameAs(mockPayment);
        }
    }

    @Test
    void createPixPayment_whenMPApiExceptionThrown_shouldThrowBadRequestException() throws Exception {
        MPResponse response = mock(MPResponse.class);
        when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(response.getContent()).thenReturn("{\"error\":\"invalid_cpf\"}");

        MPApiException exception = mock(MPApiException.class);
        when(exception.getApiResponse()).thenReturn(response);

        try (MockedConstruction<PaymentClient> mockedClient = mockConstruction(PaymentClient.class,
                (mock, context) -> doThrow(exception).when(mock).create(any()))) {

            assertThatThrownBy(() -> paymentService.createPixPayment(
                    BigDecimal.TEN, "Description", "payer@email.com",
                    "Payer Name", "12345678909", 1L
            )).isInstanceOf(BadRequestException.class)
              .hasMessage("Falha ao gerar o PIX no Mercado Pago. Tente novamente mais tarde.");
        }
    }

    @Test
    void createPixPayment_whenGenericExceptionThrown_shouldThrowBadRequestException() throws Exception {
        try (MockedConstruction<PaymentClient> mockedClient = mockConstruction(PaymentClient.class,
                (mock, context) -> doThrow(new RuntimeException("API Connection timeout")).when(mock).create(any()))) {

            assertThatThrownBy(() -> paymentService.createPixPayment(
                    BigDecimal.TEN, "Description", "payer@email.com",
                    "Payer Name", "12345678909", 1L
            )).isInstanceOf(BadRequestException.class)
              .hasMessage("Falha ao gerar o PIX no Mercado Pago. Tente novamente mais tarde.");
        }
    }

    @Test
    void getPayment_whenSuccessful_shouldReturnPayment() throws Exception {
        Payment mockPayment = mock(Payment.class);

        try (MockedConstruction<PaymentClient> mockedClient = mockConstruction(PaymentClient.class,
                (mock, context) -> when(mock.get(anyLong())).thenReturn(mockPayment))) {

            Payment result = paymentService.getPayment(12345L);
            assertThat(result).isSameAs(mockPayment);
        }
    }

    @Test
    void getPayment_whenExceptionThrown_shouldReturnNull() throws Exception {
        try (MockedConstruction<PaymentClient> mockedClient = mockConstruction(PaymentClient.class,
                (mock, context) -> doThrow(new RuntimeException("Error")).when(mock).get(anyLong()))) {

            Payment result = paymentService.getPayment(12345L);
            assertThat(result).isNull();
        }
    }
}
