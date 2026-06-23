package com.cristiane.salon.models.featureflag.controller;

import com.cristiane.salon.models.featureflag.entity.FeatureFlag;
import com.cristiane.salon.models.featureflag.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "FeatureFlag", description = "Endpoints para gerenciamento de Feature Flags")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @GetMapping("/feature-flags")
    @Operation(summary = "Lista todas as feature flags ativas/disponíveis (público)")
    public ResponseEntity<List<FeatureFlag>> getPublicFeatureFlags() {
        return ResponseEntity.ok(featureFlagService.findAll());
    }

    @GetMapping("/sysadmin/feature-flags")
    @PreAuthorize("hasAnyRole('SYSADMIN')")
    @Operation(summary = "Lista todas as feature flags (Sysadmin)")
    public ResponseEntity<List<FeatureFlag>> getAllFeatureFlags() {
        return ResponseEntity.ok(featureFlagService.findAll());
    }

    @PatchMapping("/sysadmin/feature-flags/{name}/toggle")
    @PreAuthorize("hasAnyRole('SYSADMIN')")
    @Operation(summary = "Alterna o estado de uma feature flag (Sysadmin)")
    public ResponseEntity<FeatureFlag> toggleFeatureFlag(@PathVariable String name) {
        FeatureFlag updated = featureFlagService.toggle(name);
        return ResponseEntity.ok(updated);
    }
}
