package com.cristiane.salon.integrations.payment.service;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.resources.payment.Payment;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class MercadoPagoGatewayTest {

    private final MercadoPagoGateway gateway = new MercadoPagoGateway();

    @Test
    void createPayment_delegatesToPaymentClient() throws Exception {
        Payment mockPayment = mock(Payment.class);
        try (MockedConstruction<PaymentClient> mocked = mockConstruction(PaymentClient.class,
                (mock, context) -> when(mock.create(any(), any())).thenReturn(mockPayment))) {

            Payment result = gateway.createPayment(PaymentCreateRequest.builder().build(), "idem-key-1");

            assertThat(result).isSameAs(mockPayment);
        }
    }

    @Test
    void createPayment_sendsIdempotencyKeyAsCustomHeader() throws Exception {
        Payment mockPayment = mock(Payment.class);
        try (MockedConstruction<PaymentClient> mocked = mockConstruction(PaymentClient.class,
                (mock, context) -> when(mock.create(any(), any())).thenReturn(mockPayment))) {

            gateway.createPayment(PaymentCreateRequest.builder().build(), "idem-key-42");

            PaymentClient client = mocked.constructed().get(0);
            org.mockito.ArgumentCaptor<com.mercadopago.core.MPRequestOptions> optionsCaptor =
                    org.mockito.ArgumentCaptor.forClass(com.mercadopago.core.MPRequestOptions.class);
            verify(client).create(any(), optionsCaptor.capture());
            assertThat(optionsCaptor.getValue().getCustomHeaders())
                    .containsEntry("X-Idempotency-Key", "idem-key-42");
        }
    }

    @Test
    void createPayment_whenPaymentClientThrows_propagatesTheRawException() throws Exception {
        // Precisa propagar a exceção crua (sem mapear pra BadRequestException aqui) para o
        // Resilience4j conseguir diferenciar falha transitória de recusa de negócio —
        // MercadoPagoPaymentService é quem faz esse mapeamento, não o gateway.
        try (MockedConstruction<PaymentClient> mocked = mockConstruction(PaymentClient.class,
                (mock, context) -> doThrow(new RuntimeException("Timeout")).when(mock).create(any(), any()))) {

            assertThatThrownBy(() -> gateway.createPayment(PaymentCreateRequest.builder().build(), "idem-key-1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Timeout");
        }
    }

    @Test
    void getPayment_delegatesToPaymentClient() throws Exception {
        Payment mockPayment = mock(Payment.class);
        try (MockedConstruction<PaymentClient> mocked = mockConstruction(PaymentClient.class,
                (mock, context) -> when(mock.get(anyLong())).thenReturn(mockPayment))) {

            Payment result = gateway.getPayment(1L);

            assertThat(result).isSameAs(mockPayment);
        }
    }

    @Test
    void getPaymentFallback_returnsNullInsteadOfPropagating() {
        // Preserva o comportamento histórico de MercadoPagoPaymentService.getPayment: se o
        // Mercado Pago não responder mesmo após retry/circuito, devolve null (chamador trata
        // igual a "id inválido").
        Payment result = (Payment) ReflectionTestUtils.invokeMethod(
                gateway, "getPaymentFallback", 1L, new RuntimeException("boom"));

        assertThat(result).isNull();
    }
}
