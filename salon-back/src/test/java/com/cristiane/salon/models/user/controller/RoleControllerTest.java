package com.cristiane.salon.models.user.controller;

import com.cristiane.salon.controllers.BaseControllerTest;
import com.cristiane.salon.models.user.dto.PermissionResponse;
import com.cristiane.salon.models.user.dto.RolePermissionsResponse;
import com.cristiane.salon.models.user.service.RoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleController.class)
class RoleControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RoleService roleService;

    private static final PermissionResponse PERM_1 = new PermissionResponse(
            1L, "Listar Usuários", "GET", "/v1/users", "Usuário"
    );

    private static final RolePermissionsResponse ROLE_RESPONSE = new RolePermissionsResponse(
            1L, "ADMIN", List.of(PERM_1)
    );

    @Test
    @WithMockUser(username = "sysadmin@salao.com", roles = {"SYSADMIN"})
    void getAllRoles_shouldReturn200WithRolesList() throws Exception {
        when(roleService.findAllRolesWithPermissions()).thenReturn(List.of(ROLE_RESPONSE));

        mvc.perform(get("/v1/roles").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roleName").value("ADMIN"))
                .andExpect(jsonPath("$[0].permissions[0].name").value("Listar Usuários"));
    }

    @Test
    @WithMockUser(username = "admin@salao.com", roles = {"ADMIN"})
    void getAllRoles_shouldReturn200ForAdminToo() throws Exception {
        when(roleService.findAllRolesWithPermissions()).thenReturn(List.of(ROLE_RESPONSE));

        mvc.perform(get("/v1/roles").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // Nota: a autorização real de @verifyUserPermissions.userOwnResourceOrHasPermission(...)
    // não é exercitada neste slice (@WebMvcTest não habilita @EnableMethodSecurity — ver
    // ErrorScenariosTest). A cobertura de negação de acesso vive em VerifyUserPermissionsTest.

    @Test
    @WithMockUser(username = "sysadmin@salao.com", roles = {"SYSADMIN"})
    void getAllPermissions_shouldReturn200WithPermissionsList() throws Exception {
        when(roleService.findAllPermissions()).thenReturn(List.of(PERM_1));

        mvc.perform(get("/v1/roles/permissions").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Listar Usuários"));
    }

    @Test
    @WithMockUser(username = "sysadmin@salao.com", roles = {"SYSADMIN"})
    void grantPermission_shouldReturn200() throws Exception {
        when(roleService.grantPermission(1L, 2L)).thenReturn(ROLE_RESPONSE);

        mvc.perform(post("/v1/roles/1/permissions/2").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleName").value("ADMIN"));
    }

    @Test
    @WithMockUser(username = "sysadmin@salao.com", roles = {"SYSADMIN"})
    void revokePermission_shouldReturn200() throws Exception {
        RolePermissionsResponse revoked = new RolePermissionsResponse(1L, "ADMIN", List.of());
        when(roleService.revokePermission(1L, 1L)).thenReturn(revoked);

        mvc.perform(delete("/v1/roles/1/permissions/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.permissions").isEmpty());
    }
}
