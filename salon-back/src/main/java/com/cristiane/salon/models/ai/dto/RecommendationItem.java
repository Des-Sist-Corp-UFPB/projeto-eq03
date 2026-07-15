package com.cristiane.salon.models.ai.dto;

import com.cristiane.salon.models.ai.entity.RecommendationPriority;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Um insight individual devolvido pelo modelo. {@code @JsonIgnoreProperties(ignoreUnknown = true)}
 * descarta qualquer campo extra que o modelo tente incluir — parte da defesa contra saída fora do
 * contrato esperado (ver {@link com.cristiane.salon.models.ai.service.RecommendationService}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendationItem(
        String title,
        String description,
        String suggestedAction,
        RecommendationPriority priority
) {}
