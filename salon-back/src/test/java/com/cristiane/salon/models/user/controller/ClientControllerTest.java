package com.cristiane.salon.models.user.controller;

import com.cristiane.salon.controllers.BaseControllerTest;
import com.cristiane.salon.models.user.dto.ClientDetailsResponse;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
class ClientControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void findAllClientsReturnsPage() throws Exception {
        UserResponse response = new UserResponse(1L, "Client John", "john@client.com", "12345678", null, "CLIENTE", true, LocalDateTime.now());
        org.springframework.data.domain.Page<UserResponse> page = new org.springframework.data.domain.PageImpl<>(List.of(response));
        when(userService.findAllClients(any(), any())).thenReturn(page);

        mvc.perform(get("/v1/clients")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Client John"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findClientDetailsByIdReturnsDetails() throws Exception {
        ClientDetailsResponse details = new ClientDetailsResponse(
                1L, "Client John", "john@client.com", "12345678", "12345678909", "CLIENTE", true, LocalDateTime.now(),
                5L, LocalDateTime.now(), List.of()
        );
        when(userService.findClientDetailsById(eq(1L))).thenReturn(details);

        mvc.perform(get("/v1/clients/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Client John"))
                .andExpect(jsonPath("$.totalAppointments").value(5));
    }
}
