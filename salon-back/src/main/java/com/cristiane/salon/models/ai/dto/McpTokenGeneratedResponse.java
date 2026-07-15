package com.cristiane.salon.models.ai.dto;

/** Devolvido só na criação — é a única vez que o valor em texto puro do token existe fora do cliente. */
public record McpTokenGeneratedResponse(McpTokenResponse token, String rawValue) {}
