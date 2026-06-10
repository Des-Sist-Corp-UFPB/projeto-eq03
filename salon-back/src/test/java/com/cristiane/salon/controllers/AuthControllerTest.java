package com.cristiane.salon.controllers;

import com.cristiane.salon.controller.AuthController;
import com.cristiane.salon.models.audit.AuditLogService;
import com.cristiane.salon.models.user.service.AuthService;
import com.cristiane.salon.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void loginReturns200_whenValid() throws Exception {
        when(authService.login(any())).thenReturn(null);

        String body = "{\"email\":\"a@b.com\",\"password\":\"123456\"}";

        mvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void loginReturns400_whenInvalid() throws Exception {
        String body = "{}";

        mvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }
}
