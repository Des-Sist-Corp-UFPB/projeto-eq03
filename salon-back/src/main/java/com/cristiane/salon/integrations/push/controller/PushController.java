package com.cristiane.salon.integrations.push.controller;

import com.cristiane.salon.integrations.push.dto.PushSubscribeRequest;
import com.cristiane.salon.integrations.push.dto.PushUnsubscribeRequest;
import com.cristiane.salon.integrations.push.entity.PushSubscription;
import com.cristiane.salon.integrations.push.repository.PushSubscriptionRepository;
import com.cristiane.salon.models.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Assinatura/cancelamento de notificações Web Push (issue #110). Qualquer usuário autenticado
 * pode gerenciar a própria assinatura — não é ação administrativa.
 */
@RestController
@RequestMapping("/v1/push")
@RequiredArgsConstructor
@Tag(name = "Push", description = "Assinatura de notificações Web Push")
public class PushController {

    private final PushSubscriptionRepository repository;

    @PostMapping("/subscribe")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Registra (ou reafirma) a assinatura de push deste navegador")
    public ResponseEntity<Void> subscribe(@Valid @RequestBody PushSubscribeRequest request,
            HttpServletRequest httpRequest) {
        User user = currentUser();

        PushSubscription subscription = repository.findByUserIdAndEndpoint(user.getId(), request.endpoint())
                .orElseGet(PushSubscription::new);
        subscription.setUser(user);
        subscription.setEndpoint(request.endpoint());
        subscription.setP256dh(request.p256dh());
        subscription.setAuth(request.auth());
        subscription.setUserAgent(httpRequest.getHeader("User-Agent"));
        repository.save(subscription);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/unsubscribe")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Remove a assinatura de push deste navegador")
    public ResponseEntity<Void> unsubscribe(@Valid @RequestBody PushUnsubscribeRequest request) {
        User user = currentUser();
        repository.deleteByUserIdAndEndpoint(user.getId(), request.endpoint());
        return ResponseEntity.noContent().build();
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        throw new IllegalStateException("Usuário autenticado não encontrado no contexto de segurança");
    }
}
