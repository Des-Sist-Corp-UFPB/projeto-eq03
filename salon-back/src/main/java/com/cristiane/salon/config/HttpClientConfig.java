package com.cristiane.salon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Timeout compartilhado para toda chamada HTTP a um sistema externo (Resend, provedor de IA,
 * e qualquer integração nova). Sem isso, uma dependência externa lenta ou travada consome uma
 * thread da aplicação indefinidamente em vez de falhar rápido — o que, sob carga, esgota o pool
 * de threads e derruba funcionalidades sem nenhuma relação com a integração que travou.
 *
 * Uma integração nova só precisa injetar este {@link RestClient.Builder} e chamar
 * {@code .clone().baseUrl(...)} — não precisa configurar o próprio timeout.
 */
@Configuration
public class HttpClientConfig {

    @Value("${app.http-client.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${app.http-client.read-timeout-ms:5000}")
    private int readTimeoutMs;

    @Bean
    public RestClient.Builder externalRestClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder().requestFactory(requestFactory);
    }
}
