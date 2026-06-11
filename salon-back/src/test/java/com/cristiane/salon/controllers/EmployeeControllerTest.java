package com.cristiane.salon.controllers;

import com.cristiane.salon.controller.EmployeeController;
import com.cristiane.salon.models.employee.service.EmployeeService;
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

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void createReturns201_whenValid() throws Exception {
        when(employeeService.create(any())).thenReturn(null);

        String body = "{\"userId\":1}";

        mvc.perform(post("/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void createReturns400_whenInvalid() throws Exception {
        String body = "{}";

        mvc.perform(post("/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }
}
