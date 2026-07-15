package com.cristiane.salon.models.ai.controller;

import com.cristiane.salon.annotation.Auditable;
import com.cristiane.salon.models.ai.dto.McpTokenCreateRequest;
import com.cristiane.salon.models.ai.dto.McpTokenGeneratedResponse;
import com.cristiane.salon.models.ai.dto.McpTokenResponse;
import com.cristiane.salon.models.ai.service.McpTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/sysadmin/ai-config/tokens")
@RequiredArgsConstructor
@Tag(name = "MCP Tokens", description = "Tokens de acesso ao servidor MCP — Sysadmin")
public class McpTokenController {

    private final McpTokenService mcpTokenService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSADMIN')")
    @Operation(summary = "Lista os tokens MCP (Sysadmin)")
    public ResponseEntity<List<McpTokenResponse>> list() {
        return ResponseEntity.ok(mcpTokenService.list());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSADMIN')")
    @Auditable(action = "MCP_TOKEN_CREATED", entityType = "McpAccessToken", captureArgs = true)
    @Operation(summary = "Gera um novo token MCP — valor em texto puro só nesta resposta (Sysadmin)")
    public ResponseEntity<McpTokenGeneratedResponse> generate(@Valid @RequestBody McpTokenCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mcpTokenService.generate(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSADMIN')")
    @Auditable(action = "MCP_TOKEN_REVOKED", entityType = "McpAccessToken", captureArgs = false)
    @Operation(summary = "Revoga um token MCP (Sysadmin)")
    public ResponseEntity<Void> revoke(@PathVariable Long id) {
        mcpTokenService.revoke(id);
        return ResponseEntity.noContent().build();
    }
}
