package com.cristiane.salon.integrations.push.dto;

import jakarta.validation.constraints.NotBlank;

/** Só precisa do endpoint — diferente de {@link PushSubscribeRequest}, não faz sentido exigir
 * p256dh/auth pra remover uma subscription que já existe. */
public record PushUnsubscribeRequest(@NotBlank String endpoint) {
}
