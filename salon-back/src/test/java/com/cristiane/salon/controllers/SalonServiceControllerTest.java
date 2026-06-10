package com.cristiane.salon.controllers;

import com.cristiane.salon.models.service.service.SalonServiceManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SalonServiceControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SalonServiceManager salonServiceManager;

    @Autowired
    private com.cristiane.salon.security.VerifyUserPermissions verifyUserPermissions;

    @Test
    @WithMockUser
    void createReturns201_whenValid() throws Exception {
        when(verifyUserPermissions.userOwnResourceOrHasPermission(null)).thenReturn(true);
        when(salonServiceManager.create(any())).thenReturn(null);

        String body = "{\"name\":\"cut\",\"durationMin\":30}";

        mvc.perform(post("/v1/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void createReturns400_whenInvalid() throws Exception {
        String body = "{}";

        mvc.perform(post("/v1/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public SalonServiceManager salonServiceManager() {
            return Mockito.mock(SalonServiceManager.class);
        }

        @Bean
        public com.cristiane.salon.security.VerifyUserPermissions verifyUserPermissions() {
            return Mockito.mock(com.cristiane.salon.security.VerifyUserPermissions.class);
        }

        @Bean
        public MockMvc mockMvc(WebApplicationContext wac) {
            return MockMvcBuilders.webAppContextSetup(wac).build();
        }
    }
}
