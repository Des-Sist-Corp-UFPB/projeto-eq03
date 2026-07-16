package com.cristiane.salon.models.ai.controller;

import com.cristiane.salon.annotation.Auditable;
import com.cristiane.salon.models.ai.dto.AiConfigRequest;
import com.cristiane.salon.models.ai.dto.AiConfigResponse;
import com.cristiane.salon.models.ai.dto.AiConfigTestRequest;
import com.cristiane.salon.models.ai.dto.AiConfigTestResponse;
import com.cristiane.salon.models.ai.service.AiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/sysadmin/ai-config")
@RequiredArgsConstructor
@Tag(name = "AI Config", description = "Configuração do provedor de IA (Central de IA) — Sysadmin")
public class AiConfigController {

    private final AiConfigService aiConfigService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSADMIN')")
    @Operation(summary = "Consulta a configuração de IA (Sysadmin)")
    public ResponseEntity<AiConfigResponse> get() {
        return ResponseEntity.ok(aiConfigService.get());
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SYSADMIN')")
    @Auditable(action = "AI_CONFIG_UPDATED", entityType = "AiConfig", captureArgs = false)
    @Operation(summary = "Atualiza a configuração de IA (Sysadmin)")
    public ResponseEntity<AiConfigResponse> update(@Valid @RequestBody AiConfigRequest request) {
        return ResponseEntity.ok(aiConfigService.update(request));
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('SYSADMIN')")
    @Auditable(action = "AI_CONFIG_TESTED", entityType = "AiConfig", captureArgs = false)
    @Operation(summary = "Testa a conectividade com o provedor de IA usando os valores atuais do formulário (Sysadmin)")
    public ResponseEntity<AiConfigTestResponse> testConnection(@Valid @RequestBody AiConfigTestRequest request) {
        return ResponseEntity.ok(aiConfigService.testConnection(request));
    }
}
