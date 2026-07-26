package com.cristiane.salon.integrations.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.mercadopago.MercadoPagoConfig;

import jakarta.annotation.PostConstruct;

@Configuration
public class MercadoPagoConfiguration {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    // Mesmas propriedades de timeout usadas por HttpClientConfig — o SDK do Mercado Pago
    // gerencia seu próprio HttpClient internamente, então o timeout precisa ser configurado
    // por fora (não dá pra injetar o RestClient.Builder compartilhado aqui).
    @Value("${app.http-client.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${app.http-client.read-timeout-ms:5000}")
    private int readTimeoutMs;

    @PostConstruct
    public void init() {
        if (accessToken == null || accessToken.trim().isEmpty() || accessToken.startsWith("${")) {
            throw new IllegalStateException("ERRO CRÍTICO: O Access Token do Mercado Pago não foi configurado nas variáveis de ambiente!");
        }
        // Inicializa o SDK globalmente
        MercadoPagoConfig.setAccessToken(accessToken);
        MercadoPagoConfig.setConnectionTimeout(connectTimeoutMs);
        MercadoPagoConfig.setSocketTimeout(readTimeoutMs);
        MercadoPagoConfig.setConnectionRequestTimeout(connectTimeoutMs);
    }
}
