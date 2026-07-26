package com.cristiane.salon.integrations.email.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Único ponto de contato com a API HTTP do provedor de e-mail (Resend). Isolado do
 * {@link EmailService} pelo mesmo motivo do {@code MercadoPagoGateway}: os métodos de
 * {@link EmailService} já são {@code @Async} e chamavam um método HTTP privado dentro da MESMA
 * classe — anotações de resiliência nesse método privado seriam ignoradas pelo proxy do Spring
 * (chamada interna não passa pelo proxy). {@link EmailService} continua responsável por decidir
 * o que fazer quando o envio falha (hoje: registrar em auditoria e seguir em frente — o
 * agendamento em si nunca depende do e-mail ser entregue).
 */
@Component
@RequiredArgsConstructor
public class EmailGateway {

    private final RestClient.Builder restClientBuilder;

    @Value("${mail.password}")
    private String apiKey;

    @Value("${mail.from:notificacoes@elksandro.com}")
    private String fromEmail;

    @Value("${mail.api-url}")
    private String apiUrl;

    @CircuitBreaker(name = "email-provider")
    @Retry(name = "email-provider")
    public void send(String to, String subject, String htmlContent, String replyTo) {
        RestClient restClient = restClientBuilder.clone().baseUrl(apiUrl).build();

        Map<String, Object> payload = Map.of(
                "from", "Cristiane Salon <" + fromEmail + ">",
                "to", new String[]{to},
                "subject", subject,
                "html", htmlContent,
                "reply_to", replyTo
        );

        restClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
