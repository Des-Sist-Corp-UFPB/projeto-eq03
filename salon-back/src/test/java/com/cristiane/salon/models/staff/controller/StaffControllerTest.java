package com.cristiane.salon.models.staff.controller;

import com.cristiane.salon.controllers.BaseControllerTest;
import com.cristiane.salon.models.staff.dto.StaffPixQrCodeResponse;
import com.cristiane.salon.models.staff.dto.StaffProfileResponse;
import com.cristiane.salon.models.staff.enums.BrazilianState;
import com.cristiane.salon.models.staff.service.StaffPixService;
import com.cristiane.salon.models.staff.service.StaffProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Nota: a autorização real de {@code @verifyUserPermissions.userOwnResourceOrHasPermission(...)}
 * não é exercitada neste slice ({@code @WebMvcTest} não habilita {@code @EnableMethodSecurity}
 * — ver RoleControllerTest para o mesmo padrão). Este teste cobre contrato HTTP/JSON e
 * validação de request; a autorização em si é responsabilidade de VerifyUserPermissionsTest.
 */
@WebMvcTest(StaffController.class)
class StaffControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private StaffProfileService staffProfileService;

    @MockitoBean
    private StaffPixService staffPixService;

    private StaffProfileResponse sampleResponse() {
        return new StaffProfileResponse(
                1L, 10L, "Maria", "maria@example.com", "FUNCIONARIA", true,
                "Maria Silva", null, "Maria Silva", "***.***.777-35",
                LocalDate.of(1990, 1, 1), null,
                "81999999999", null, null,
                "50000-000", "Rua A", "10", null, "Boa Vista", "Recife", BrazilianState.PE,
                null, null, false,
                LocalDate.now(), null, LocalDateTime.now(), null
        );
    }

    @Test
    @WithMockUser
    void create_whenValidRequest_shouldReturn201WithoutRawCpfOrPixKeyInResponse() throws Exception {
        when(staffProfileService.create(any())).thenReturn(sampleResponse());

        String body = """
                {
                  "name": "Maria", "email": "maria@example.com", "password": "Senha@123",
                  "roleName": "FUNCIONARIA", "fullName": "Maria Silva",
                  "cpf": "111.444.777-35", "birthDate": "1990-01-01",
                  "phone": "(81) 99999-9999",
                  "zipCode": "50000-000", "street": "Rua A", "streetNumber": "10",
                  "district": "Boa Vista", "city": "Recife", "stateUf": "PE",
                  "remunerationType": "SALARIO_FIXO", "remunerationValue": 2000
                }
                """;

        mvc.perform(post("/v1/staff").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpfMasked").value("***.***.777-35"))
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.pixKey").doesNotExist());
    }

    @Test
    @WithMockUser
    void create_whenMissingRequiredFields_shouldReturn400() throws Exception {
        mvc.perform(post("/v1/staff").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void findAll_shouldReturnPageOfStaffProfiles() throws Exception {
        when(staffProfileService.findAll(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mvc.perform(get("/v1/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Maria Silva"));
    }

    @Test
    @WithMockUser
    void findById_shouldReturnStaffProfile() throws Exception {
        when(staffProfileService.findById(1L)).thenReturn(sampleResponse());

        mvc.perform(get("/v1/staff/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser
    void generatePixQrCode_shouldReturnPayloadWithoutPixKeyField() throws Exception {
        when(staffPixService.generateQrCode(eq(1L), any()))
                .thenReturn(new StaffPixQrCodeResponse("00020101...", new BigDecimal("70.00"), "Maria Silva"));

        String body = """
                { "amount": 70.00, "description": "Salário" }
                """;

        mvc.perform(post("/v1/staff/1/pix-qrcode").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brCodePayload").value("00020101..."))
                .andExpect(jsonPath("$.pixKey").doesNotExist());
    }

    @Test
    @WithMockUser
    void generatePixQrCode_whenAmountIsZeroOrNegative_shouldReturn400() throws Exception {
        String body = """
                { "amount": 0 }
                """;

        mvc.perform(post("/v1/staff/1/pix-qrcode").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
