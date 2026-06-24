package com.cristiane.salon.models.user.controller;

import com.cristiane.salon.models.user.dto.PermissionResponse;
import com.cristiane.salon.models.user.dto.RolePermissionsResponse;
import com.cristiane.salon.models.user.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/roles")
@RequiredArgsConstructor
@Tag(name = "RBAC", description = "Gerenciamento dinâmico de permissões por cargo (SYSADMIN)")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "Lista todos os cargos com suas permissões")
    @PreAuthorize("hasAuthority('GET:/v1/roles')")
    public ResponseEntity<List<RolePermissionsResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.findAllRolesWithPermissions());
    }

    @GetMapping("/permissions")
    @Operation(summary = "Lista todas as permissões disponíveis no sistema")
    @PreAuthorize("hasAuthority('GET:/v1/roles/permissions')")
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        return ResponseEntity.ok(roleService.findAllPermissions());
    }

    @PostMapping("/{roleId}/permissions/{permissionId}")
    @Operation(summary = "Concede uma permissão a um cargo")
    @PreAuthorize("hasAuthority('POST:/v1/roles/{roleId}/permissions/{permissionId}')")
    public ResponseEntity<RolePermissionsResponse> grantPermission(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        return ResponseEntity.ok(roleService.grantPermission(roleId, permissionId));
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @Operation(summary = "Revoga uma permissão de um cargo")
    @PreAuthorize("hasAuthority('DELETE:/v1/roles/{roleId}/permissions/{permissionId}')")
    public ResponseEntity<RolePermissionsResponse> revokePermission(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        return ResponseEntity.ok(roleService.revokePermission(roleId, permissionId));
    }
}
