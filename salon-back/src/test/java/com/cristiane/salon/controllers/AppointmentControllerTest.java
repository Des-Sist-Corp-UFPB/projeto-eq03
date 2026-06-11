package com.cristiane.salon.controllers;

import com.cristiane.salon.controller.AppointmentController;
import com.cristiane.salon.models.appointment.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @Test
    @WithMockUser
    void createReturns201_whenValid() throws Exception {
        when(appointmentService.create(any())).thenReturn(null);

        String body = "{\"employeeId\":1,\"serviceId\":1}";

        mvc.perform(post("/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void createReturns400_whenInvalid() throws Exception {
        String body = "{}";

        mvc.perform(post("/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    // Disabled because method security is disabled in WebMvcTest slice
    // @Test
    // @WithMockUser
    // void createReturns403_whenNoPermission() throws Exception {
    //     String body = "{\"employeeId\":1,\"serviceId\":1}";
    //     mvc.perform(post("/v1/appointments")
    //             .contentType(MediaType.APPLICATION_JSON)
    //             .content(body))
    //             .andExpect(status().isForbidden());
    // }
}
