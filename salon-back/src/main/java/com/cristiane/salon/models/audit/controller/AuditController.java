package com.cristiane.salon.models.audit.controller;

import com.cristiane.salon.models.audit.AuditLog;
import com.cristiane.salon.models.audit.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Endpoints para logs de auditoria (Admin)")
public class AuditController {
    
    private final AuditLogService auditLogService;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('SYSADMIN')")
    @Operation(summary = "Lista todos os logs de auditoria com paginação e filtros combinados")
    public ResponseEntity<Page<AuditLog>> getAllAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<AuditLog> logs = auditLogService.getAuditLogsWithCombinedFilters(
                userId, entityType, action, startDate, endDate, pageable
        );
        return ResponseEntity.ok(logs);
    }

    
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('SYSADMIN')")
    @Operation(summary = "Lista logs de auditoria de um usuário específico")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<AuditLog> logs = auditLogService.getAuditLogsByUser(userId, pageRequest);
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/action/{action}")
    @PreAuthorize("hasAnyRole('SYSADMIN')")
    @Operation(summary = "Lista logs de auditoria por tipo de ação")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<AuditLog> logs = auditLogService.getAuditLogsByAction(action, pageRequest);
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/entity/{entityType}")
    @PreAuthorize("hasAnyRole('SYSADMIN')")
    @Operation(summary = "Lista logs de auditoria por tipo de entidade")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByEntityType(
            @PathVariable String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<AuditLog> logs = auditLogService.getAuditLogsByEntityType(entityType, pageRequest);
        return ResponseEntity.ok(logs);
    }
    
    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('SYSADMIN')")
    @Operation(summary = "Lista logs de auditoria por período de datas")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<AuditLog> logs = auditLogService.getAuditLogsByDateRange(from, to, pageRequest);
        return ResponseEntity.ok(logs);
    }
}
