package com.cristiane.salon.models.user.controller;

import com.cristiane.salon.controllers.BaseControllerTest;

import com.cristiane.salon.models.user.controller.UserController;
import com.cristiane.salon.models.user.dto.UserResponse;
import com.cristiane.salon.models.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser
    void createReturns201_whenValid() throws Exception {
        when(userService.create(any())).thenReturn(null);

        String body = "{\"name\":\"xyz\",\"email\":\"a@b.com\",\"password\":\"123456\",\"roleId\":1}";

        mvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void createReturns400_whenInvalid() throws Exception {
        String body = "{}";

        mvc.perform(post("/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findAllReturnsUsers() throws Exception {
        UserResponse response = new UserResponse(1L, "Alice", "alice@example.com", "99999999", null, "ROLE_ADMIN", true, LocalDateTime.now());
        when(userService.findAll(any())).thenReturn(List.of(response));

        mvc.perform(get("/v1/users")
                .param("includeInactive", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alice"));
    }

    @Test
    @WithMockUser
    void findByIdReturnsUser() throws Exception {
        UserResponse response = new UserResponse(2L, "Bob", "bob@example.com", "88888888", null, "CLIENTE", true, LocalDateTime.now());
        when(userService.findById(eq(2L))).thenReturn(response);

        mvc.perform(get("/v1/users/details/id/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bob"));
    }

    @Test
    @WithMockUser
    void updateReturnsUpdatedUser() throws Exception {
        UserResponse response = new UserResponse(2L, "Updated Bob", "bob@example.com", "88888888", null, "CLIENTE", true, LocalDateTime.now());
        when(userService.update(eq(2L), any())).thenReturn(response);

        String body = "{\"name\":\"Updated Bob\",\"email\":\"bob@example.com\"}";

        mvc.perform(patch("/v1/users/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Bob"));
    }

    @Test
    @WithMockUser
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(userService).delete(eq(3L));

        mvc.perform(delete("/v1/users/3")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void restoreReturnsRestoredUser() throws Exception {
        UserResponse response = new UserResponse(4L, "Restored", "restored@example.com", "77777777", null, "CLIENTE", true, LocalDateTime.now());
        when(userService.restore(eq(4L))).thenReturn(response);

        mvc.perform(patch("/v1/users/4/restore")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Restored"));
    }

    @Test
    @WithMockUser
    void updateMyCpfReturnsOk_whenValidCpf() throws Exception {
        UserResponse response = new UserResponse(1L, "Alice", "alice@example.com", "99999999", "12345678901", "CLIENTE", true, LocalDateTime.now());
        when(userService.updateMyCpf("12345678901")).thenReturn(response);

        String body = "{\"cpf\":\"12345678901\"}";

        mvc.perform(patch("/v1/users/me/cpf")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("12345678901"));
    }

    @Test
    @WithMockUser
    void updateMyCpfReturns400_whenCpfIsInvalid() throws Exception {
        // CPF com menos de 11 dígitos deve retornar 400 (falha na validação Bean Validation)
        String body = "{\"cpf\":\"123\"}";

        mvc.perform(patch("/v1/users/me/cpf")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }
}
