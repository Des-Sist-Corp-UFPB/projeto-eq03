package com.cristiane.salon.integrations.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// Esse record mapeia o formato do JSON que o Mercado Pago nos envia
public record MercadoPagoNotification(
        String action,
        String type,
        Data data
) {
    public record Data(@JsonProperty("id") String id) {}
}