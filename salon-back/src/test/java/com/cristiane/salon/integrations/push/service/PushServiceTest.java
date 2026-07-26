package com.cristiane.salon.integrations.push.service;

import com.cristiane.salon.integrations.push.entity.PushSubscription;
import com.cristiane.salon.integrations.push.repository.PushSubscriptionRepository;
import com.cristiane.salon.models.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushServiceTest {

    @Mock
    private PushSubscriptionRepository repository;

    @Mock
    private nl.martijndwars.webpush.PushService webPushService;

    private PushService pushService;

    // Chave pública EC (P-256) e segredo de autenticação de exemplo do RFC 8291 §5 — precisam
    // ser valores criptograficamente válidos de verdade (ponto real na curva), não texto
    // qualquer, porque o construtor de Notification faz parsing EC real. Não são segredos.
    private static final String SAMPLE_P256DH = "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4";
    private static final String SAMPLE_AUTH = "BTBZMqHH6r4Tts7J_aSIgg";

    private PushSubscription subscription(Long userId, String endpoint) {
        User user = new User();
        user.setId(userId);
        PushSubscription subscription = new PushSubscription();
        subscription.setUser(user);
        subscription.setEndpoint(endpoint);
        subscription.setP256dh(SAMPLE_P256DH);
        subscription.setAuth(SAMPLE_AUTH);
        return subscription;
    }

    private HttpResponse responseWithStatus(int status) {
        HttpResponse response = mock(HttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(status);
        when(response.getStatusLine()).thenReturn(statusLine);
        return response;
    }

    @BeforeEach
    void setUp() {
        // Em produção isso é feito por WebPushConfig.@PostConstruct — aqui não há contexto
        // Spring, então o Notification (que faz parsing EC real da chave p256dh) falharia com
        // "no such provider: BC" sem isso.
        if (java.security.Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            java.security.Security.addProvider(new BouncyCastleProvider());
        }
        pushService = new PushService(repository, webPushService, new ObjectMapper());
    }

    @Test
    void sendToUser_whenNoSubscriptions_neverCallsWebPush() throws Exception {
        when(repository.findByUserId(1L)).thenReturn(List.of());

        pushService.sendToUser(1L, "Título", "Corpo", "/url");

        verifyNoInteractions(webPushService);
    }

    @Test
    void sendToUser_whenSuccessful_sendsNotificationWithJsonPayload() throws Exception {
        PushSubscription subscription = subscription(1L, "https://push.example.com/abc");
        when(repository.findByUserId(1L)).thenReturn(List.of(subscription));
        HttpResponse response = responseWithStatus(201);
        when(webPushService.send(any(Notification.class))).thenReturn(response);

        pushService.sendToUser(1L, "Agendamento confirmado", "Seu horário foi confirmado", "/my-appointments");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(webPushService).send(captor.capture());
        assertThat(captor.getValue().getEndpoint()).isEqualTo("https://push.example.com/abc");
        assertThat(new String(captor.getValue().getPayload()))
                .contains("Agendamento confirmado")
                .contains("Seu horário foi confirmado")
                .contains("/my-appointments");
        verify(repository, never()).deleteByUserIdAndEndpoint(any(), any());
    }

    @Test
    void sendToUser_whenSubscriptionExpired410_removesSubscriptionFromRepository() throws Exception {
        PushSubscription subscription = subscription(1L, "https://push.example.com/expired");
        when(repository.findByUserId(1L)).thenReturn(List.of(subscription));
        HttpResponse response = responseWithStatus(410);
        when(webPushService.send(any(Notification.class))).thenReturn(response);

        pushService.sendToUser(1L, "Título", "Corpo", "/url");

        verify(repository).deleteByUserIdAndEndpoint(1L, "https://push.example.com/expired");
    }

    @Test
    void sendToUser_whenOneSubscriptionFailsWithException_stillProcessesTheOthers() throws Exception {
        PushSubscription broken = subscription(1L, "https://push.example.com/broken");
        PushSubscription healthy = subscription(1L, "https://push.example.com/healthy");
        when(repository.findByUserId(1L)).thenReturn(List.of(broken, healthy));
        HttpResponse response = responseWithStatus(201);
        when(webPushService.send(any(Notification.class)))
                .thenThrow(new RuntimeException("Falha de rede"))
                .thenReturn(response);

        pushService.sendToUser(1L, "Título", "Corpo", "/url");

        verify(webPushService, times(2)).send(any(Notification.class));
        verify(repository, never()).deleteByUserIdAndEndpoint(any(), any());
    }

    @Test
    void sendToUser_whenNon410ErrorStatus_doesNotRemoveSubscription() throws Exception {
        PushSubscription subscription = subscription(1L, "https://push.example.com/abc");
        when(repository.findByUserId(1L)).thenReturn(List.of(subscription));
        HttpResponse response = responseWithStatus(500);
        when(webPushService.send(any(Notification.class))).thenReturn(response);

        pushService.sendToUser(1L, "Título", "Corpo", "/url");

        verify(repository, never()).deleteByUserIdAndEndpoint(any(), any());
    }
}
