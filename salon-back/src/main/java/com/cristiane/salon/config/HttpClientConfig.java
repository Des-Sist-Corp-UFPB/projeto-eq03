package com.cristiane.salon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Timeout compartilhado para toda chamada HTTP a um sistema externo (Resend, Mercado Pago,
 * e qualquer integração nova). Sem isso, uma dependência externa lenta ou travada consome uma
 * thread da aplicação indefinidamente em vez de falhar rápido — o que, sob carga, esgota o pool
 * de threads e derruba funcionalidades sem nenhuma relação com a integração que travou.
 *
 * Uma integração nova só precisa injetar este {@link RestClient.Builder} e chamar
 * {@code .clone().baseUrl(...)} — não precisa configurar o próprio timeout.
 *
 * O provedor de IA é a exceção deliberada: ver {@link #aiRestClientBuilder()} logo abaixo.
 */
@Configuration
public class HttpClientConfig {

    @Value("${app.http-client.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${app.http-client.read-timeout-ms:5000}")
    private int readTimeoutMs;

    @Value("${app.ai-http-client.read-timeout-ms:30000}")
    private int aiReadTimeoutMs;

    // @Primary: MercadoPagoConfiguration e EmailGateway injetam RestClient.Builder sem
    // @Qualifier — precisam continuar resolvendo pra este bean sem precisar mudar nada.
    @Primary
    @Bean
    public RestClient.Builder externalRestClientBuilder() {
        return builderWithTimeouts(connectTimeoutMs, readTimeoutMs);
    }

    /**
     * Timeout à parte para o provedor de IA (issue: recomendação falhando em produção com
     * "I/O error ... : null" — o log mostrava a falha chegando exatos 5000ms após o envio,
     * batendo com o timeout padrão acima). Gerar uma resposta de LLM tem um perfil de
     * latência fundamentalmente diferente de Mercado Pago/Resend: rotineiramente leva vários
     * segundos, ainda mais atravessando um proxy compartilhado (ex.: LiteLLM da turma) que
     * pode estar enfileirando chamadas de outros grupos. Usar o timeout de 5s genérico aqui
     * fazia a aplicação desistir antes mesmo do provedor terminar de responder.
     */
    @Bean
    public RestClient.Builder aiRestClientBuilder() {
        return builderWithTimeouts(connectTimeoutMs, aiReadTimeoutMs);
    }

    private RestClient.Builder builderWithTimeouts(int connectMs, int readMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectMs))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readMs));

        return RestClient.builder().requestFactory(requestFactory);
    }
}
