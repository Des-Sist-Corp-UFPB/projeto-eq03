package com.cristiane.salon.models.ai.dto;

import com.cristiane.salon.models.ai.entity.McpAccessToken;

import java.time.Instant;

/** Nunca carrega o hash nem o valor do token — só metadados de gestão. */
public record McpTokenResponse(
        Long id,
        String name,
        String createdBy,
        Instant createdAt,
        Instant expiresAt,
        Instant lastUsedAt,
        boolean revoked
) {
    public static McpTokenResponse fromEntity(McpAccessToken token) {
        return new McpTokenResponse(
                token.getId(),
                token.getName(),
                token.getCreatedBy(),
                token.getCreatedAt(),
                token.getExpiresAt(),
                token.getLastUsedAt(),
                Boolean.TRUE.equals(token.getRevoked())
        );
    }
}
