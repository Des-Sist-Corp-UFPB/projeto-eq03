package com.cristiane.salon.models.user.service;

import com.cristiane.salon.exception.BadRequestException;
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

    /**
     * SYSADMIN e ADMIN têm acesso total resolvido de forma hardcoded em
     * {@link com.cristiane.salon.security.VerifyUserPermissions} (independente da tabela
     * tb_role_permissions). Conceder/revogar permissões desses cargos pelo painel RBAC não
     * teria nenhum efeito real de autorização — por isso eles ficam de fora da listagem e
     * não podem ser alvo de grant/revoke, para não passar uma falsa sensação de controle.
     */
    private static final Set<String> ROLES_WITH_HARDCODED_ACCESS = Set.of("SYSADMIN", "ADMIN");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Retorna os Roles com suas permissões associadas, exceto SYSADMIN e ADMIN
     * (acesso total garantido pelo sistema, não configurável via RBAC).
     * Utilizado pelo painel RBAC do SYSADMIN.
     */
    @Transactional(readOnly = true)
    public List<RolePermissionsResponse> findAllRolesWithPermissions() {
        return roleRepository.findAll().stream()
                .filter(role -> !ROLES_WITH_HARDCODED_ACCESS.contains(role.getName()))
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
        Role role = findEditableRole(roleId);

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
        Role role = findEditableRole(roleId);

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permissão não encontrada"));

        role.getPermissions().remove(permission);
        Role saved = roleRepository.save(role);
        return toRolePermissionsResponse(saved);
    }

    // ---- Helpers ----

    /**
     * Busca um role garantindo que não é um dos cargos com acesso total hardcoded
     * (SYSADMIN/ADMIN), já que alterar permissões deles não surtiria efeito real.
     */
    private Role findEditableRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role não encontrada"));

        if (ROLES_WITH_HARDCODED_ACCESS.contains(role.getName())) {
            throw new BadRequestException(
                    "As permissões do cargo " + role.getName() +
                    " não podem ser alteradas: o acesso total já é garantido pelo sistema, " +
                    "independentemente da tabela de permissões.");
        }
        return role;
    }

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
