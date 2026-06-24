package com.cristiane.salon.models.user.dto;

/**
 * DTO que representa uma permissão individual no sistema RBAC.
 * Enviado ao frontend para exibição no painel de gerenciamento de permissões.
 */
public record PermissionResponse(
        Long id,
        String name,
        String httpMethod,
        String endpoint,
        String classe
) {}
