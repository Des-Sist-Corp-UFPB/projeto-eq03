package com.cristiane.salon.controller;

import com.cristiane.salon.annotation.Auditable;
import com.cristiane.salon.models.product.dto.ProductRequest;
import com.cristiane.salon.models.product.dto.ProductResponse;
import com.cristiane.salon.models.product.service.ProductService;
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
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Endpoints para gerenciamento de produtos")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Lista todos os produtos (Público)")
    public ResponseEntity<List<ProductResponse>> findAll(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(productService.findAll(active));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um produto por ID (Público)")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMapping
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "CREATE", entityType = "PRODUCT", captureArgs = true)
    @Operation(summary = "Cria um novo produto (Admin)")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "UPDATE", entityType = "PRODUCT", captureArgs = true)
    @Operation(summary = "Atualiza um produto (Admin)")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "DELETE", entityType = "PRODUCT", captureArgs = true)
    @Operation(summary = "Remove um produto (Admin)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "RESTORE", entityType = "PRODUCT", captureArgs = true)
    @Operation(summary = "Reativa um produto (Admin)")
    public ResponseEntity<ProductResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(productService.reactivate(id));
    }
}

