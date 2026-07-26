package com.cristiane.salon.integrations.email.outbox.controller;

import com.cristiane.salon.annotation.Auditable;
import com.cristiane.salon.integrations.email.outbox.dto.EmailOutboxFilter;
import com.cristiane.salon.integrations.email.outbox.dto.EmailOutboxResponse;
import com.cristiane.salon.integrations.email.outbox.service.EmailOutboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Central de E-mails: fila de retry + histórico recente de envio (ver README, seção "Fila de
 * e-mail (outbox) e retenção", para a política completa). Reenvio manual ({@code POST .../resend})
 * é restrito a ADMIN/SYSADMIN por decisão de produto — não há permissão concedida a
 * GERENTE_DE_ATENDIMENTO para esse método (ver V36).
 */
@RestController
@RequestMapping("/v1/email-outbox")
@RequiredArgsConstructor
@Tag(name = "Email Outbox", description = "Fila de retry e histórico recente de envio de e-mail")
public class EmailOutboxController {

    private final EmailOutboxService emailOutboxService;

    @GetMapping
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Operation(summary = "Lista envios de e-mail recentes, com filtro por status (Admin/Gerente)")
    public ResponseEntity<Page<EmailOutboxResponse>> findAll(
            EmailOutboxFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(emailOutboxService.findAll(filter, pageable));
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("@verifyUserPermissions.userOwnResourceOrHasPermission(null)")
    @Auditable(action = "RESEND", entityType = "EmailOutboxEntry")
    @Operation(summary = "Força o reenvio imediato de um e-mail da fila, ignorando o backoff (Admin/Sysadmin)")
    public ResponseEntity<EmailOutboxResponse> resend(@PathVariable Long id) {
        return ResponseEntity.ok(emailOutboxService.resendNow(id));
    }
}
