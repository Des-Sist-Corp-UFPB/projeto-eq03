package com.cristiane.salon.models.salonprofile.controller;

import com.cristiane.salon.annotation.Auditable;
import com.cristiane.salon.models.salonprofile.dto.SalonProfileResponse;
import com.cristiane.salon.models.salonprofile.dto.SalonProfileUpdateRequest;
import com.cristiane.salon.models.salonprofile.service.SalonProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Perfil público do salão (issue #117) + horário de funcionamento (issue #116).
 *
 * <p>{@code GET /v1/salon/profile} é público (sem login) — consumido pela página inicial e pelo
 * wizard de agendamento do cliente. {@code PUT /v1/admin/salon/profile} é restrito a ADMIN/
 * SYSADMIN por decisão de produto: não há permissão concedida a GERENTE_DE_ATENDIMENTO (ver
 * V42) — só passam pelo bypass automático do {@code VerifyUserPermissions}.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Salon Profile", description = "Perfil público do salão e horário de funcionamento")
public class SalonProfileController {

    private final SalonProfileService salonProfileService;

    @GetMapping("/v1/salon/profile")
    @Operation(summary = "Perfil público do salão (sobre, endereço, redes sociais, horário de funcionamento)")
    public ResponseEntity<SalonProfileResponse> getPublicProfile() {
        return ResponseEntity.ok(salonProfileService.getProfile());
    }

    @PutMapping("/v1/admin/salon/profile")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "UPDATE", entityType = "SalonProfile", captureArgs = true)
    @Operation(summary = "Atualiza o perfil do salão e o horário de funcionamento (Admin/Sysadmin)")
    public ResponseEntity<SalonProfileResponse> updateProfile(@Valid @RequestBody SalonProfileUpdateRequest request) {
        return ResponseEntity.ok(salonProfileService.updateProfile(request));
    }
}
