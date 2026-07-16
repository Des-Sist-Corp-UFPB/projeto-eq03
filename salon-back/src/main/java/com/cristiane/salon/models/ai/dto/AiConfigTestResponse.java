package com.cristiane.salon.models.ai.dto;

/**
 * Resultado de um teste de conectividade ad-hoc — nunca afeta {@code tb_ai_call_log} nem o
 * orçamento diário (essa contagem é só para chamadas reais de recomendação); é um diagnóstico
 * independente, disponível mesmo com a feature flag ou a Central de IA desativadas.
 */
public record AiConfigTestResponse(boolean success, String message, Long latencyMs) {}
