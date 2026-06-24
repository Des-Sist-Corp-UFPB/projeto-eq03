package com.cristiane.salon.models.user.dto;

import java.util.List;

/**
 * DTO que retorna um Role com suas permissões associadas.
 * Usado pelo painel RBAC do SYSADMIN.
 */
public record RolePermissionsResponse(
        Long roleId,
        String roleName,
        List<PermissionResponse> permissions
) {}
