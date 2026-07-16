package com.cristiane.salon.models.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Testa os valores atuais do formulário (mesmo antes de salvar) — {@code apiKey} em branco
 * significa "usar a chave já salva na Central de IA", igual ao comportamento do PUT normal.
 */
public record AiConfigTestRequest(
        @NotBlank(message = "A URL base é obrigatória")
        String baseUrl,

        @NotBlank(message = "O modelo é obrigatório")
        String model,

        String apiKey
) {}
