package com.cristiane.salon.models.ai.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * {@code apiKey} é opcional: quando em branco, a chave já cifrada em banco é mantida —
 * permite alternar {@code enabled}/ajustar limites sem precisar redigitar o segredo.
 */
public record AiConfigRequest(
        @NotBlank(message = "A URL base é obrigatória")
        String baseUrl,

        @NotBlank(message = "O modelo é obrigatório")
        String model,

        String apiKey,

        @NotNull(message = "A temperatura é obrigatória")
        @DecimalMin(value = "0.0", message = "A temperatura mínima é 0.0")
        @DecimalMax(value = "1.0", message = "A temperatura máxima é 1.0")
        BigDecimal temperature,

        @NotNull(message = "O limite de tokens é obrigatório")
        @Min(value = 50, message = "O limite mínimo de tokens é 50")
        @Max(value = 4000, message = "O limite máximo de tokens é 4000")
        Integer maxTokens,

        @NotNull(message = "O campo 'enabled' é obrigatório")
        Boolean enabled,

        @NotNull(message = "O orçamento diário é obrigatório")
        @Min(value = 1, message = "O orçamento diário deve ser de pelo menos 1 chamada")
        Integer dailyCallBudget
) {}
