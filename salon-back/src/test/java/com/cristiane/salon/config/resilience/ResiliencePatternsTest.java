package com.cristiane.salon.config.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa o PADRÃO de resiliência adotado para toda integração externa (Mercado Pago, provedor
 * de e-mail, provedor de IA) — não uma integração específica. As configs aqui espelham os
 * valores de {@code default} em application.yaml (resilience4j.circuitbreaker/retry), então
 * este teste garante que o comportamento (quantas tentativas, quando o circuito abre, o que
 * ele ignora) é realmente o que a documentação do README promete, simulando falha real de um
 * "sistema externo" via um Supplier que sempre lança exceção.
 */
class ResiliencePatternsTest {

    @Test
    void retry_whenTransientFailureThenSuccess_shouldRetryUntilItSucceeds() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1))
                .build();
        Retry retry = Retry.of("test-transient", config);

        AtomicInteger attempts = new AtomicInteger(0);
        Supplier<String> flakyExternalCall = () -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("Timeout simulando serviço externo instável");
            }
            return "ok";
        };

        String result = Retry.decorateSupplier(retry, flakyExternalCall).get();

        assertThat(result).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void retry_whenAlwaysFails_shouldGiveUpAfterMaxAttempts() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1))
                .build();
        Retry retry = Retry.of("test-always-fails", config);

        AtomicInteger attempts = new AtomicInteger(0);
        Supplier<String> alwaysFails = () -> {
            attempts.incrementAndGet();
            throw new RuntimeException("Serviço externo fora do ar");
        };

        assertThatThrownBy(() -> Retry.decorateSupplier(retry, alwaysFails).get())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Serviço externo fora do ar");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void retry_whenExceptionIsIgnored_shouldNotRetryBusinessRejection() {
        // Espelha o caso do Mercado Pago: MPApiException (CPF inválido, saldo insuficiente) é
        // recusa de negócio, não indisponibilidade do gateway — não vale a pena tentar de novo.
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1))
                .ignoreExceptions(IllegalArgumentException.class)
                .build();
        Retry retry = Retry.of("test-ignore-business-error", config);

        AtomicInteger attempts = new AtomicInteger(0);
        Supplier<String> businessRejection = () -> {
            attempts.incrementAndGet();
            throw new IllegalArgumentException("CPF inválido");
        };

        assertThatThrownBy(() -> Retry.decorateSupplier(retry, businessRejection).get())
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    void circuitBreaker_afterEnoughFailures_shouldOpenAndFailFastWithoutCallingTheSupplier() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("test-circuit", config);

        AtomicInteger externalCalls = new AtomicInteger(0);
        Supplier<String> alwaysFails = Retry.decorateSupplier(
                Retry.of("no-retry", RetryConfig.custom().maxAttempts(1).build()),
                () -> {
                    externalCalls.incrementAndGet();
                    throw new RuntimeException("Serviço externo fora do ar");
                }
        );
        Supplier<String> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, alwaysFails);

        // minimumNumberOfCalls = 5 falhas consecutivas -> circuito deve abrir
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(decorated::get).isInstanceOf(RuntimeException.class);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(externalCalls.get()).isEqualTo(5);

        // Com o circuito aberto, a próxima chamada nem tenta o serviço externo de novo — falha
        // rápido com CallNotPermittedException. É exatamente isso que evita esgotar o pool de
        // threads da aplicação quando o serviço externo está fora do ar.
        assertThatThrownBy(decorated::get).isInstanceOf(CallNotPermittedException.class);
        assertThat(externalCalls.get()).isEqualTo(5);
    }

    @Test
    void circuitBreaker_whenExceptionIsIgnored_shouldNotCountTowardOpeningTheCircuit() {
        // Espelha o caso da IA: IllegalStateException (resposta fora do schema esperado) é
        // problema de conteúdo, não indica que o provedor está fora do ar.
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .ignoreExceptions(IllegalStateException.class)
                .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of("test-ignore-content-error", config);

        Supplier<String> malformedResponse = () -> {
            throw new IllegalStateException("Resposta do modelo fora do schema esperado");
        };
        Supplier<String> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, malformedResponse);

        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(decorated::get).isInstanceOf(IllegalStateException.class);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
