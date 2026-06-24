package com.cristiane.salon.models.user.dto;

import java.util.List;

/**
 * DTO retornado pelo endpoint GET /v1/auth/me.
 * Contém o perfil do usuário autenticado e a lista de permissões (strings METHOD:ENDPOINT)
 * lidas diretamente do banco de dados para garantir consistência.
 */
public record UserProfileResponse(
        Long userId,
        String email,
        String name,
        String role,
        String cpf,
        List<String> permissions
) {}
