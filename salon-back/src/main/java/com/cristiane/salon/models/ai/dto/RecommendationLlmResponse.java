package com.cristiane.salon.models.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Formato esperado do JSON devolvido pelo modelo — ver {@code RecommendationPromptBuilder.SYSTEM_PROMPT}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendationLlmResponse(List<RecommendationItem> recommendations) {}
