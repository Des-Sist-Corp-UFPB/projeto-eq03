package com.cristiane.salon.models.salonprofile.controller;

import com.cristiane.salon.controllers.BaseControllerTest;
import com.cristiane.salon.models.salonprofile.dto.BusinessHourDto;
import com.cristiane.salon.models.salonprofile.dto.SalonProfileResponse;
import com.cristiane.salon.models.salonprofile.service.SalonProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Nota: como em outros slices, a autorização real do {@code @PreAuthorize} não é exercitada
 * aqui ({@code @WebMvcTest} não habilita {@code @EnableMethodSecurity}) — cobre contrato HTTP/
 * JSON. O GET não exige autenticação nem aqui nem em produção (rota pública).
 */
@WebMvcTest(SalonProfileController.class)
class SalonProfileControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SalonProfileService salonProfileService;

    private SalonProfileResponse sampleResponse() {
        List<BusinessHourDto> hours = List.of(
                new BusinessHourDto(DayOfWeek.MONDAY, true, LocalTime.of(8, 0), LocalTime.of(18, 0)),
                new BusinessHourDto(DayOfWeek.SUNDAY, false, null, null)
        );
        return new SalonProfileResponse(1L, "Espaço Cristiane Moura", "Sobre nós",
                "Rua Teste, 123", "83999999999", "@salao", "83999999999", null, null, hours);
    }

    @Test
    void getPublicProfile_returnsProfileWithBusinessHours() throws Exception {
        when(salonProfileService.getProfile()).thenReturn(sampleResponse());

        mvc.perform(get("/v1/salon/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Espaço Cristiane Moura"))
                .andExpect(jsonPath("$.businessHours[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.businessHours[1].open").value(false));
    }

    @Test
    void updateProfile_whenValid_returnsUpdatedProfile() throws Exception {
        when(salonProfileService.updateProfile(any())).thenReturn(sampleResponse());

        String body = """
                {
                  "name": "Espaço Cristiane Moura",
                  "description": "Sobre nós",
                  "address": "Rua Teste, 123",
                  "phone": "83999999999",
                  "instagram": "@salao",
                  "whatsapp": "83999999999",
                  "businessHours": [
                    {"dayOfWeek": "MONDAY", "open": true, "openTime": "08:00:00", "closeTime": "18:00:00"},
                    {"dayOfWeek": "TUESDAY", "open": true, "openTime": "08:00:00", "closeTime": "18:00:00"},
                    {"dayOfWeek": "WEDNESDAY", "open": true, "openTime": "08:00:00", "closeTime": "18:00:00"},
                    {"dayOfWeek": "THURSDAY", "open": true, "openTime": "08:00:00", "closeTime": "18:00:00"},
                    {"dayOfWeek": "FRIDAY", "open": true, "openTime": "08:00:00", "closeTime": "18:00:00"},
                    {"dayOfWeek": "SATURDAY", "open": true, "openTime": "08:00:00", "closeTime": "18:00:00"},
                    {"dayOfWeek": "SUNDAY", "open": false, "openTime": null, "closeTime": null}
                  ]
                }
                """;

        mvc.perform(put("/v1/admin/salon/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Espaço Cristiane Moura"));
    }

    @Test
    void updateProfile_whenNameBlank_returns400() throws Exception {
        String body = """
                {"name": "", "businessHours": []}
                """;

        mvc.perform(put("/v1/admin/salon/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
