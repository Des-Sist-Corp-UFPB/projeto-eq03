package com.cristiane.salon.models.featureflag.controller;

import com.cristiane.salon.controllers.BaseControllerTest;

import com.cristiane.salon.models.featureflag.controller.FeatureFlagController;
import com.cristiane.salon.models.featureflag.entity.FeatureFlag;
import com.cristiane.salon.models.featureflag.service.FeatureFlagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeatureFlagController.class)
class FeatureFlagControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FeatureFlagService featureFlagService;

    @Test
    @WithMockUser
    void getPublicFeatureFlagsReturnsFlags() throws Exception {
        FeatureFlag flag = new FeatureFlag("TEST_FLAG", true, "Description");
        when(featureFlagService.findAll()).thenReturn(List.of(flag));

        mvc.perform(get("/v1/feature-flags")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("TEST_FLAG"))
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    void getAllFeatureFlagsReturnsFlags() throws Exception {
        FeatureFlag flag = new FeatureFlag("ADMIN_FLAG", false, "Admin description");
        when(featureFlagService.findAll()).thenReturn(List.of(flag));

        mvc.perform(get("/v1/sysadmin/feature-flags")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("ADMIN_FLAG"))
                .andExpect(jsonPath("$[0].enabled").value(false));
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    void toggleFeatureFlagReturnsToggled() throws Exception {
        FeatureFlag flag = new FeatureFlag("TOGGLED_FLAG", true, "Description");
        when(featureFlagService.toggle(anyString())).thenReturn(flag);

        mvc.perform(patch("/v1/sysadmin/feature-flags/TOGGLED_FLAG/toggle")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TOGGLED_FLAG"))
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
