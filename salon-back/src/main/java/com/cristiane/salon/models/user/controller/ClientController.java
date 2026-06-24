package com.cristiane.salon.models.user.controller;

import com.cristiane.salon.models.user.dto.ClientDetailsResponse;
import com.cristiane.salon.models.user.dto.ClientFilter;
import com.cristiane.salon.models.user.dto.UserResponse;
import com.cristiane.salon.models.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Clients", description = "Endpoints para gerenciamento comercial de clientes")
public class ClientController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Lista todos os clientes com filtros e paginação (Admin/Gerente)")
    public ResponseEntity<Page<UserResponse>> findAll(
            @Valid ClientFilter filter,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userService.findAllClients(filter, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Busca detalhes de histórico de um cliente por ID (Admin/Gerente)")
    public ResponseEntity<ClientDetailsResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findClientDetailsById(id));
    }
}
