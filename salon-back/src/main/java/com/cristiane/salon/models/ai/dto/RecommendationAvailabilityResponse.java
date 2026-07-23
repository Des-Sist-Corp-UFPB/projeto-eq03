package com.cristiane.salon.models.ai.dto;

/**
 * Diagnóstico leve para ADMIN/GERENTE (que não têm acesso a {@code /v1/sysadmin/ai-config})
 * saberem se {@link com.cristiane.salon.models.ai.service.RecommendationService#generate}
 * vai funcionar agora, sem expor nenhum dado sensível da configuração (URL, modelo, key).
 */
public record RecommendationAvailabilityResponse(boolean available) {}
