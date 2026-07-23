package com.cristiane.salon.models.ai.controller;

import com.cristiane.salon.models.ai.dto.RecommendationAvailabilityResponse;
import com.cristiane.salon.models.ai.dto.RecommendationResult;
import com.cristiane.salon.models.ai.entity.RecommendationType;
import com.cristiane.salon.models.ai.service.RecommendationService;
import com.cristiane.salon.models.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin/recommendations")
@RequiredArgsConstructor
@Tag(name = "AI Recommendations", description = "Recomendações de IA (financeiro/ocupação, retenção de clientes)")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/status")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Diagnóstico leve: diz se é possível gerar recomendações agora, sem expor a configuração sensível (Admin/Gerente)")
    public ResponseEntity<RecommendationAvailabilityResponse> status() {
        return ResponseEntity.ok(new RecommendationAvailabilityResponse(recommendationService.isAvailable()));
    }

    @GetMapping("/{type}")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Consulta a última recomendação gerada para o tipo, sem chamar o provedor de IA (Admin/Gerente)")
    public ResponseEntity<RecommendationResult> getLatest(@PathVariable RecommendationType type) {
        return ResponseEntity.ok(recommendationService.getLatestCached(type));
    }

    @PostMapping("/{type}/generate")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Gera uma nova recomendação chamando o provedor de IA (Admin/Gerente)")
    public ResponseEntity<RecommendationResult> generate(@PathVariable RecommendationType type) {
        return ResponseEntity.ok(recommendationService.generate(type, "USER", currentUserId()));
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return String.valueOf(user.getId());
        }
        return "unknown";
    }
}
