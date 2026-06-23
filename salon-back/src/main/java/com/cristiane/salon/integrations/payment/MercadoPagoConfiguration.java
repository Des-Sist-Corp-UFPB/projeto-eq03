package com.cristiane.salon.integrations.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.mercadopago.MercadoPagoConfig;

import jakarta.annotation.PostConstruct;

@Configuration
public class MercadoPagoConfiguration {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @PostConstruct
    public void init() {
        if (accessToken == null || accessToken.trim().isEmpty() || accessToken.startsWith("${")) {
            throw new IllegalStateException("ERRO CRÍTICO: O Access Token do Mercado Pago não foi configurado nas variáveis de ambiente!");
        }
        // Inicializa o SDK globalmente
        MercadoPagoConfig.setAccessToken(accessToken);
    }
}
