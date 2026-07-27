package com.cristiane.salon.integrations.email.outbox.controller;

import com.cristiane.salon.controllers.BaseControllerTest;
import com.cristiane.salon.integrations.email.outbox.dto.EmailOutboxResponse;
import com.cristiane.salon.integrations.email.outbox.enums.EmailOutboxStatus;
import com.cristiane.salon.integrations.email.outbox.service.EmailOutboxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;

/**
 * Nota: como em StaffControllerTest, a autorização real do RBAC não é exercitada neste slice
 * ({@code @WebMvcTest} não habilita {@code @EnableMethodSecurity}) — cobre contrato HTTP/JSON.
 */
@WebMvcTest(EmailOutboxController.class)
class EmailOutboxControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmailOutboxService emailOutboxService;

    private EmailOutboxResponse sampleResponse(EmailOutboxStatus status) {
        return new EmailOutboxResponse(
                1L, "cliente@example.com", "Assunto", status, 1,
                null, null, "Appointment", 42L,
                Instant.now(), status == EmailOutboxStatus.SENT ? Instant.now() : null
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void findAll_shouldReturnPagedList() throws Exception {
        when(emailOutboxService.findAll(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleResponse(EmailOutboxStatus.SENT))));

        mvc.perform(get("/v1/email-outbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].recipientEmail").value("cliente@example.com"))
                .andExpect(jsonPath("$.content[0].status").value("SENT"))
                .andExpect(jsonPath("$.content[0].htmlContent").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void resend_shouldReturnUpdatedEntry() throws Exception {
        when(emailOutboxService.resendNow(1L)).thenReturn(sampleResponse(EmailOutboxStatus.SENT));

        mvc.perform(post("/v1/email-outbox/1/resend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
    }
}
