package com.cristiane.salon.integrations.payment.controller;

import com.cristiane.salon.controllers.BaseControllerTest;

import com.cristiane.salon.integrations.payment.controller.MercadoPagoWebhookController;
import com.cristiane.salon.models.appointment.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MercadoPagoWebhookController.class)
class WebhookControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @Test
    @WithMockUser
    void receiveNotification_whenNoSignature_returns403() throws Exception {
        String body = "{\"action\":\"payment.created\",\"type\":\"payment\",\"data\":{\"id\":\"123456\"}}";

        mvc.perform(post("/v1/webhooks/mercadopago")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden());

        verify(appointmentService, never()).processPixPaymentWebhook(anyLong());
    }

    @Test
    @WithMockUser
    void receiveNotification_whenInvalidSignature_returns403() throws Exception {
        String body = "{\"action\":\"payment.created\",\"type\":\"payment\",\"data\":{\"id\":\"123456\"}}";

        mvc.perform(post("/v1/webhooks/mercadopago")
                .header("x-signature", "invalid_signature")
                .header("x-request-id", "some-req-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden());

        verify(appointmentService, never()).processPixPaymentWebhook(anyLong());
    }

    @Test
    @WithMockUser
    void receiveNotification_whenValidSignatureAndNotPayment_returns200_doesNotCallService() throws Exception {
        // Notification of type "merchant_order" instead of "payment"
        String body = "{\"action\":\"created\",\"type\":\"merchant_order\",\"data\":{\"id\":\"123456\"}}";

        mvc.perform(post("/v1/webhooks/mercadopago")
                .header("x-signature", "valid_sig_abc")
                .header("x-request-id", "some-req-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        verify(appointmentService, never()).processPixPaymentWebhook(anyLong());
    }

    @Test
    @WithMockUser
    void receiveNotification_whenValidSignatureAndPayment_returns200_callsService() throws Exception {
        String body = "{\"action\":\"payment.updated\",\"type\":\"payment\",\"data\":{\"id\":\"987654321\"}}";

        mvc.perform(post("/v1/webhooks/mercadopago")
                .header("x-signature", "valid_sig_xyz")
                .header("x-request-id", "some-req-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        verify(appointmentService, times(1)).processPixPaymentWebhook(987654321L);
    }

    @Test
    @WithMockUser
    void receiveNotification_whenJsonMalformed_returns200_doesNotThrow() throws Exception {
        String body = "{ malformed json ";

        mvc.perform(post("/v1/webhooks/mercadopago")
                .header("x-signature", "valid_sig_xyz")
                .header("x-request-id", "some-req-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        verify(appointmentService, never()).processPixPaymentWebhook(anyLong());
    }
}
