package com.cristiane.salon.models.user.controller;

import com.cristiane.salon.controllers.BaseControllerTest;

import com.cristiane.salon.models.user.controller.AuthController;
import com.cristiane.salon.models.user.dto.TokenResponse;
import com.cristiane.salon.models.user.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void loginReturns200_whenValid() throws Exception {
        TokenResponse response = new TokenResponse("accessToken", "refreshToken");
        when(authService.login(any())).thenReturn(response);

        String body = "{\"email\":\"a@b.com\",\"password\":\"123456\"}";

        mvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"));
    }

    @Test
    void loginReturns400_whenInvalid() throws Exception {
        String body = "{}";

        mvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerReturns200_whenValid() throws Exception {
        TokenResponse response = new TokenResponse("accessToken", "refreshToken");
        when(authService.register(any())).thenReturn(response);

        String body = "{\"name\":\"Alice\",\"email\":\"alice@b.com\",\"password\":\"123456\"}";

        mvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"));
    }

    @Test
    void registerReturns400_whenInvalid() throws Exception {
        String body = "{}";

        mvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshReturns200_whenValid() throws Exception {
        TokenResponse response = new TokenResponse("newAccessToken", "newRefreshToken");
        when(authService.refresh(any())).thenReturn(response);

        String body = "{\"refreshToken\":\"oldRefreshToken\"}";

        mvc.perform(post("/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccessToken"));
    }

    @Test
    void refreshReturns400_whenInvalid() throws Exception {
        String body = "{}";

        mvc.perform(post("/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }
}
