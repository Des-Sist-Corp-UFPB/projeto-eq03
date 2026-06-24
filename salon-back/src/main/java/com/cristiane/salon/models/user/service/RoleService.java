package com.cristiane.salon.models.user.service;

import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.user.dto.PermissionResponse;
import com.cristiane.salon.models.user.dto.RolePermissionsResponse;
import com.cristiane.salon.models.user.entity.Permission;
import com.cristiane.salon.models.user.entity.Role;
import com.cristiane.salon.models.user.repository.PermissionRepository;
import com.cristiane.salon.models.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Retorna todos os Roles com suas permissões associadas.
     * Utilizado pelo painel RBAC do SYSADMIN.
     */
    @Transactional(readOnly = true)
    public List<RolePermissionsResponse> findAllRolesWithPermissions() {
        return roleRepository.findAll().stream()
                .map(this::toRolePermissionsResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retorna todas as permissões disponíveis no sistema.
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> findAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Concede uma permissão a um role.
     * Idempotente: não lança erro se a permissão já estiver associada.
     */
    @Transactional
    public RolePermissionsResponse grantPermission(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role não encontrada"));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada"));

        role.getPermissions().add(permission);
        Role saved = roleRepository.save(role);
        return toRolePermissionsResponse(saved);
    }

    /**
     * Revoga uma permissão de um role.
     */
    @Transactional
    public RolePermissionsResponse revokePermission(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role não encontrada"));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada"));

        role.getPermissions().remove(permission);
        Role saved = roleRepository.save(role);
        return toRolePermissionsResponse(saved);
    }

    // ---- Helpers ----

    private RolePermissionsResponse toRolePermissionsResponse(Role role) {
        List<PermissionResponse> perms = role.getPermissions().stream()
                .map(this::toPermissionResponse)
                .sorted((a, b) -> {
                    int cmp = a.classe().compareTo(b.classe());
                    if (cmp != 0) return cmp;
                    return a.name().compareTo(b.name());
                })
                .collect(Collectors.toList());

        return new RolePermissionsResponse(role.getId(), role.getName(), perms);
    }

    private PermissionResponse toPermissionResponse(Permission p) {
        return new PermissionResponse(p.getId(), p.getName(), p.getHttpMethod(), p.getEndpoint(), p.getClasse());
    }
}
