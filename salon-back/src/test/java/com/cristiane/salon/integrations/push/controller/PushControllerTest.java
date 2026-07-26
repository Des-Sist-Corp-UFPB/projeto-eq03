package com.cristiane.salon.integrations.push.controller;

import com.cristiane.salon.controllers.BaseControllerTest;
import com.cristiane.salon.integrations.push.entity.PushSubscription;
import com.cristiane.salon.integrations.push.repository.PushSubscriptionRepository;
import com.cristiane.salon.models.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autentica com o principal REAL do app ({@link User}, não o genérico do
 * {@code @WithMockUser}) porque {@code PushController.currentUser()} espelha o mesmo padrão já
 * usado em {@code RecommendationController} — o principal É a entidade User (ela implementa
 * UserDetails diretamente), não um username separado.
 */
@WebMvcTest(PushController.class)
class PushControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PushSubscriptionRepository repository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("cliente@example.com");
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor asUser() {
        return authentication(new UsernamePasswordAuthenticationToken(user, null, java.util.List.of()));
    }

    @Test
    void subscribe_whenNewEndpoint_createsSubscription() throws Exception {
        when(repository.findByUserIdAndEndpoint(eq(1L), any())).thenReturn(Optional.empty());

        mvc.perform(MockMvcRequestBuilders.post("/v1/push/subscribe")
                        .with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "TestAgent/1.0")
                        .content("""
                                {"endpoint":"https://push.example.com/abc","p256dh":"p256dh-key","auth":"auth-key"}
                                """))
                .andExpect(status().isOk());

        verify(repository).save(argThat((PushSubscription s) ->
                s.getEndpoint().equals("https://push.example.com/abc")
                        && s.getP256dh().equals("p256dh-key")
                        && s.getAuth().equals("auth-key")
                        && s.getUserAgent().equals("TestAgent/1.0")
                        && s.getUser() == user));
    }

    @Test
    void subscribe_whenEndpointAlreadyExists_updatesInPlaceInsteadOfDuplicating() throws Exception {
        PushSubscription existing = new PushSubscription();
        existing.setUser(user);
        existing.setEndpoint("https://push.example.com/abc");
        existing.setP256dh("old-key");
        existing.setAuth("old-auth");
        when(repository.findByUserIdAndEndpoint(1L, "https://push.example.com/abc")).thenReturn(Optional.of(existing));

        mvc.perform(MockMvcRequestBuilders.post("/v1/push/subscribe")
                        .with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endpoint":"https://push.example.com/abc","p256dh":"new-key","auth":"new-auth"}
                                """))
                .andExpect(status().isOk());

        verify(repository).save(existing);
        org.assertj.core.api.Assertions.assertThat(existing.getP256dh()).isEqualTo("new-key");
    }

    @Test
    void subscribe_whenMissingFields_returns400() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/v1/push/subscribe")
                        .with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(repository);
    }

    @Test
    void unsubscribe_removesTheSubscriptionForTheCurrentUser() throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete("/v1/push/unsubscribe")
                        .with(asUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endpoint":"https://push.example.com/abc"}
                                """))
                .andExpect(status().isNoContent());

        verify(repository).deleteByUserIdAndEndpoint(1L, "https://push.example.com/abc");
    }
}
