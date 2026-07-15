package com.cristiane.salon.models.ai.dto;

import com.cristiane.salon.models.ai.entity.AiConfig;
import com.cristiane.salon.security.crypto.AiEncryptionUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Nunca carrega a API key em texto puro — só uma versão mascarada, ou {@code null} se ainda não configurada. */
public record AiConfigResponse(
        String baseUrl,
        String model,
        String apiKeyMasked,
        boolean apiKeyConfigured,
        BigDecimal temperature,
        Integer maxTokens,
        boolean enabled,
        Integer dailyCallBudget,
        String updatedBy,
        LocalDateTime updatedAt
) {
    public static AiConfigResponse fromEntity(AiConfig config, String decryptedKeyForMasking) {
        return new AiConfigResponse(
                config.getBaseUrl(),
                config.getModel(),
                AiEncryptionUtil.mask(decryptedKeyForMasking),
                config.getApiKeyEncrypted() != null,
                config.getTemperature(),
                config.getMaxTokens(),
                config.getEnabled(),
                config.getDailyCallBudget(),
                config.getUpdatedBy(),
                config.getUpdatedAt()
        );
    }
}
