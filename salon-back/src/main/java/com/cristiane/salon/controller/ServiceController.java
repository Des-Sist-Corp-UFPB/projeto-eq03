package com.cristiane.salon.controller;

import com.cristiane.salon.models.service.dto.ServiceRequest;
import com.cristiane.salon.models.service.dto.ServiceResponse;
import com.cristiane.salon.models.service.service.ServiceService;
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
@RequestMapping("/v1/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Endpoints para gerenciamento de serviços de beleza")
public class ServiceController {

    private final ServiceService serviceService;

    @GetMapping
    @Operation(summary = "Lista todos os serviços (Público)")
    public ResponseEntity<List<ServiceResponse>> findAll() {
        return ResponseEntity.ok(serviceService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um serviço por ID (Público)")
    public ResponseEntity<ServiceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceService.findById(id));
    }

    @PostMapping
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Cria um novo serviço (Admin)")
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Atualiza um serviço (Admin)")
    public ResponseEntity<ServiceResponse> update(@PathVariable Long id, @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(serviceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Remove um serviço (Admin)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
