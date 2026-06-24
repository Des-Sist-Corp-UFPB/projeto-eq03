package com.cristiane.salon.models.user.service;

import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.user.dto.PermissionResponse;
import com.cristiane.salon.models.user.dto.RolePermissionsResponse;
import com.cristiane.salon.models.user.entity.Permission;
import com.cristiane.salon.models.user.entity.Role;
import com.cristiane.salon.models.user.repository.PermissionRepository;
import com.cristiane.salon.models.user.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleService roleService;

    private Permission permission1;
    private Permission permission2;
    private Role roleAdmin;
    private Role roleCliente;

    @BeforeEach
    void setUp() {
        permission1 = new Permission(1L, "Listar Usuários", "/v1/users", "GET", "Usuário");
        permission2 = new Permission(2L, "Criar Usuário", "/v1/users", "POST", "Usuário");

        roleAdmin = new Role();
        roleAdmin.setId(1L);
        roleAdmin.setName("ADMIN");
        roleAdmin.setPermissions(new HashSet<>(Set.of(permission1)));

        roleCliente = new Role();
        roleCliente.setId(2L);
        roleCliente.setName("CLIENTE");
        roleCliente.setPermissions(new HashSet<>());
    }

    @Test
    void findAllRolesWithPermissions_shouldReturnAllRoles() {
        when(roleRepository.findAll()).thenReturn(List.of(roleAdmin, roleCliente));

        List<RolePermissionsResponse> result = roleService.findAllRolesWithPermissions();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(RolePermissionsResponse::roleName)
                .containsExactlyInAnyOrder("ADMIN", "CLIENTE");

        RolePermissionsResponse adminResult = result.stream()
                .filter(r -> r.roleName().equals("ADMIN"))
                .findFirst()
                .orElseThrow();
        assertThat(adminResult.permissions()).hasSize(1);
        assertThat(adminResult.permissions().get(0).id()).isEqualTo(1L);
    }

    @Test
    void findAllPermissions_shouldReturnAllPermissions() {
        when(permissionRepository.findAll()).thenReturn(List.of(permission1, permission2));

        List<PermissionResponse> result = roleService.findAllPermissions();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PermissionResponse::name)
                .containsExactlyInAnyOrder("Listar Usuários", "Criar Usuário");
    }

    @Test
    void grantPermission_shouldAddPermissionToRole() {
        when(roleRepository.findById(2L)).thenReturn(Optional.of(roleCliente));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission1));
        when(roleRepository.save(roleCliente)).thenReturn(roleCliente);

        RolePermissionsResponse result = roleService.grantPermission(2L, 1L);

        assertThat(roleCliente.getPermissions()).contains(permission1);
        assertThat(result.roleName()).isEqualTo("CLIENTE");
        verify(roleRepository).save(roleCliente);
    }

    @Test
    void grantPermission_whenRoleNotFound_shouldThrowResourceNotFoundException() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.grantPermission(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role não encontrada");
    }

    @Test
    void grantPermission_whenPermissionNotFound_shouldThrowResourceNotFoundException() {
        when(roleRepository.findById(2L)).thenReturn(Optional.of(roleCliente));
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.grantPermission(2L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Permissão não encontrada");
    }

    @Test
    void revokePermission_shouldRemovePermissionFromRole() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(roleAdmin));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission1));
        when(roleRepository.save(roleAdmin)).thenReturn(roleAdmin);

        RolePermissionsResponse result = roleService.revokePermission(1L, 1L);

        assertThat(roleAdmin.getPermissions()).doesNotContain(permission1);
        verify(roleRepository).save(roleAdmin);
    }

    @Test
    void revokePermission_whenRoleNotFound_shouldThrowResourceNotFoundException() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.revokePermission(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role não encontrada");
    }

    @Test
    void revokePermission_whenPermissionNotFound_shouldThrowResourceNotFoundException() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(roleAdmin));
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.revokePermission(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Permissão não encontrada");
    }

    @Test
    void findAllRolesWithPermissions_withEmptyPermissions_shouldReturnEmptyList() {
        when(roleRepository.findAll()).thenReturn(List.of(roleCliente));

        List<RolePermissionsResponse> result = roleService.findAllRolesWithPermissions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).permissions()).isEmpty();
    }

    @Test
    void grantPermission_isIdempotent_addsPermissionOnce() {
        roleAdmin.setPermissions(new HashSet<>(Set.of(permission1)));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(roleAdmin));
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission1));
        when(roleRepository.save(roleAdmin)).thenReturn(roleAdmin);

        roleService.grantPermission(1L, 1L);

        // Set semantics ensure no duplicate
        assertThat(roleAdmin.getPermissions()).hasSize(1).contains(permission1);
    }
}
