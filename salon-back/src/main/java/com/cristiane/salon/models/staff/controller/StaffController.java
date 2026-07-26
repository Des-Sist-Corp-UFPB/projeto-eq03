package com.cristiane.salon.models.staff.controller;

import com.cristiane.salon.annotation.Auditable;
import com.cristiane.salon.models.staff.dto.*;
import com.cristiane.salon.models.staff.service.StaffPixService;
import com.cristiane.salon.models.staff.service.StaffProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Cadastro completo de equipe (FUNCIONARIA / GERENTE_DE_ATENDIMENTO).
 *
 * <p>A criação ({@code POST}) é restrita a ADMIN/SYSADMIN por decisão de produto: dados como
 * remuneração e chave PIX não devem ser autocadastrados pela própria pessoa sem revisão. Não
 * há permissão de RBAC concedida a GERENTE_DE_ATENDIMENTO para este método — só ADMIN e
 * SYSADMIN passam pela regra 2 do {@code VerifyUserPermissions} (ver V34).
 */
@RestController
@RequestMapping("/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff", description = "Cadastro completo de equipe (funcionárias e gerentes de atendimento)")
public class StaffController {

    private final StaffProfileService staffProfileService;
    private final StaffPixService staffPixService;

    @PostMapping
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "CREATE", entityType = "StaffProfile", captureArgs = true)
    @Operation(summary = "Cadastra um novo membro da equipe (Admin/Sysadmin)")
    public ResponseEntity<StaffProfileResponse> create(@Valid @RequestBody StaffProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(staffProfileService.create(request));
    }

    @GetMapping
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Lista cadastros de equipe com filtros e paginação (Admin/Gerente)")
    public ResponseEntity<Page<StaffProfileResponse>> findAll(
            @Valid StaffFilter filter,
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(staffProfileService.findAll(filter, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Busca um cadastro de equipe por ID (Admin/Gerente)")
    public ResponseEntity<StaffProfileResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(staffProfileService.findById(id));
    }

    @PostMapping("/{id}/pix-qrcode")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "GENERATE_PIX_QRCODE", entityType = "StaffProfile")
    @Operation(summary = "Gera o QR Code PIX para pagar este membro da equipe, sem expor a chave (Admin/Sysadmin)")
    public ResponseEntity<StaffPixQrCodeResponse> generatePixQrCode(
            @PathVariable Long id, @Valid @RequestBody GenerateStaffPixRequest request) {
        return ResponseEntity.ok(staffPixService.generateQrCode(id, request));
    }
}
