package com.cristiane.salon.integrations.payment.service;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.resources.payment.Payment;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Único ponto de contato com o SDK do Mercado Pago. Isolado em seu próprio bean (em vez de
 * viver dentro de {@link MercadoPagoPaymentService}) porque as anotações de resiliência do
 * Resilience4j são aplicadas via proxy do Spring — uma chamada de um método para outro dentro
 * da MESMA classe não passa pelo proxy, então @CircuitBreaker/@Retry seriam ignorados se
 * ficassem num método privado chamado internamente por {@link MercadoPagoPaymentService}.
 *
 * {@link MercadoPagoPaymentService} continua responsável por mapear qualquer falha (rede,
 * timeout, circuito aberto, recusa de negócio) para {@code BadRequestException} — este gateway
 * só transporta e deixa a exceção original escapar, para o Resilience4j conseguir diferenciar
 * falha transitória (retry vale a pena) de recusa de negócio (retry não adianta).
 */
@Profile("!performance")
@Component
public class MercadoPagoGateway {

    @CircuitBreaker(name = "mercadopago")
    @Retry(name = "mercadopago")
    public Payment createPayment(PaymentCreateRequest request) throws Exception {
        PaymentClient client = new PaymentClient();
        return client.create(request);
    }

    @CircuitBreaker(name = "mercadopago", fallbackMethod = "getPaymentFallback")
    @Retry(name = "mercadopago")
    public Payment getPayment(Long paymentId) throws Exception {
        PaymentClient client = new PaymentClient();
        return client.get(paymentId);
    }

    // Mantém o comportamento histórico de getPayment: se o Mercado Pago não responder mesmo
    // após retry/circuito, devolve null em vez de propagar (chamador trata id inválido/serviço
    // fora do ar da mesma forma).
    private Payment getPaymentFallback(Long paymentId, Throwable t) {
        return null;
    }
}
