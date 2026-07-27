package com.cristiane.salon.integrations.push.service;

import com.cristiane.salon.integrations.push.entity.PushSubscription;
import com.cristiane.salon.integrations.push.repository.PushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import org.apache.http.HttpResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Envia notificações Web Push para todos os dispositivos autorizados de um usuário. Nunca
 * bloqueia a requisição HTTP que a disparou ({@code @Async}), e nunca propaga falha — cada
 * assinatura é tratada de forma independente: uma subscription expirada (410 Gone) é removida
 * automaticamente do banco, e a falha em uma não impede o envio para as demais.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final PushSubscriptionRepository repository;
    private final nl.martijndwars.webpush.PushService webPushService;
    private final ObjectMapper objectMapper;

    @Async
    public void sendToUser(Long userId, String title, String body, String url) {
        List<PushSubscription> subscriptions = repository.findByUserId(userId);
        if (subscriptions.isEmpty()) {
            // Cenário normal (usuário que nunca autorizou notificações), mas precisa aparecer no
            // log: sem esta linha, "push não chegou por falta de subscription" e "push não chegou
            // por bug no envio" ficam indistinguíveis — os dois casos não deixam rastro nenhum.
            log.info("Nenhuma subscription de push registrada para o usuário {} — notificação '{}' não enviada", userId, title);
            return;
        }

        String payload = buildPayload(title, body, url);
        for (PushSubscription subscription : subscriptions) {
            sendOrCleanUp(subscription, payload);
        }
    }

    private String buildPayload(String title, String body, String url) {
        try {
            return objectMapper.writeValueAsString(Map.of("title", title, "body", body, "url", url));
        } catch (Exception e) {
            // Erro de serialização de 3 Strings não é esperado — se acontecer, é bug nosso,
            // não falha do provedor. Não faz sentido tentar enviar nada nesse caso.
            throw new IllegalStateException("Falha ao montar payload de notificação push", e);
        }
    }

    private void sendOrCleanUp(PushSubscription subscription, String payload) {
        try {
            Notification notification = new Notification(
                    subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(), payload);
            HttpResponse response = webPushService.send(notification);
            int status = response.getStatusLine().getStatusCode();

            if (status == 410) {
                repository.deleteByUserIdAndEndpoint(subscription.getUser().getId(), subscription.getEndpoint());
                log.info("Subscription de push expirada (410 Gone) removida para o usuário {}", subscription.getUser().getId());
            } else if (status >= 400) {
                log.warn("Push retornou status {} para o usuário {}", status, subscription.getUser().getId());
            }
        } catch (Exception e) {
            log.warn("Falha ao enviar push para o usuário {}: {}", subscription.getUser().getId(), e.getMessage());
        }
    }
}
