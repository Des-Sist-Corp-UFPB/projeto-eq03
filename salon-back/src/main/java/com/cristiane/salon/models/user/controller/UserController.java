package com.cristiane.salon.models.user.controller;

import com.cristiane.salon.annotation.Auditable;
import com.cristiane.salon.models.user.dto.UpdateCpfRequest;
import com.cristiane.salon.models.user.dto.UserCreateRequest;
import com.cristiane.salon.models.user.dto.UserResponse;
import com.cristiane.salon.models.user.dto.UserUpdateRequest;
import com.cristiane.salon.models.user.dto.UserCpfInfoResponse;
import com.cristiane.salon.models.user.dto.UserFilter;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints para gerenciamento de usuários")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Lista todos os usuários internos com filtros e paginação (Admin/Gerente)")
    public ResponseEntity<Page<UserResponse>> findAll(
            @Valid UserFilter filter,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(userService.findAllUsers(filter, pageable));
    }

    @PostMapping
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "CREATE", entityType = "USER", captureArgs = true)
    @Operation(summary = "Cria um novo usuário (Admin)")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(userService.create(request));
    }

    @GetMapping("/details/id/{id}")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(#id)")
    @Operation(summary = "Busca um usuário por ID (Dono ou Admin)")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(#id)")
    @Auditable(action = "UPDATE", entityType = "USER", captureArgs = true)
    @Operation(summary = "Atualiza um usuário (Dono ou Admin)")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "DELETE", entityType = "USER", captureArgs = true)
    @Operation(summary = "Remove um usuário (Admin)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "RESTORE", entityType = "USER", captureArgs = true)
    @Operation(summary = "Reativa um usuário desativado (Admin)")
    public ResponseEntity<UserResponse> restore(@PathVariable Long id) {
        return ResponseEntity.ok(userService.restore(id));
    }

    @PatchMapping("/me/cpf")
    @Operation(summary = "Atualiza o CPF do próprio usuário autenticado (JIT no pagamento PIX)")
    public ResponseEntity<UserResponse> updateMyCpf(@Valid @RequestBody UpdateCpfRequest request) {
        return ResponseEntity.ok(userService.updateMyCpf(request.cpf()));
    }

    @GetMapping("/me/cpf-info")
    @Operation(summary = "Busca informações sobre o CPF salvo do usuário autenticado")
    public ResponseEntity<UserCpfInfoResponse> getMyCpfInfo() {
        return ResponseEntity.ok(userService.getMyCpfInfo());
    }
}
