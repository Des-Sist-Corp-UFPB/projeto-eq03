package com.cristiane.salon.models.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record McpTokenCreateRequest(
        @NotBlank(message = "O nome do token é obrigatório")
        String name,

        /** Em dias; nulo = sem expiração. Limitado a 1 ano para forçar rotação periódica. */
        @Min(value = 1, message = "A validade mínima é de 1 dia")
        @Max(value = 365, message = "A validade máxima é de 365 dias")
        Integer expiresInDays
) {}
