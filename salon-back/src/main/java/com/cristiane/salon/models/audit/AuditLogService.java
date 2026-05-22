package com.cristiane.salon.models.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    
    private final AuditLogRepository auditLogRepository;
    
    public AuditLog logAction(
            Long userId,
            String userEmail,
            String action,
            String entityType,
            Long entityId,
            String details,
            String status) {
        return logAction(userId, userEmail, action, entityType, entityId, details, status, null);
    }
    
    public AuditLog logAction(
            Long userId,
            String userEmail,
            String action,
            String entityType,
            Long entityId,
            String details,
            String status,
            String errorMessage) {
        
        try {
            HttpServletRequest request = getRequest();
            String ipAddress = getClientIp(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : "N/A";
            
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status(status)
                    .errorMessage(errorMessage)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            AuditLog savedAudit = auditLogRepository.save(auditLog);
            
            logger.info("🔍 AUDITORIA | Usuário: {} | Ação: {} | Entidade: {} (ID: {}) | Status: {}",
                    userEmail, action, entityType, entityId, status);
            
            return savedAudit;
        } catch (Exception e) {
            logger.error("❌ Erro ao registrar auditoria", e);
            return null;
        }
    }
    
    public Page<AuditLog> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    public Page<AuditLog> getAllAuditLogs(String action, String entityType, String userEmail, Pageable pageable) {
        String searchAction = (action == null || action.trim().isEmpty()) ? null : action.trim();
        String searchEntity = (entityType == null || entityType.trim().isEmpty()) ? null : entityType.trim();
        
        String searchEmail = null;
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            searchEmail = "%" + userEmail.trim().toLowerCase() + "%";
        }
        
        return auditLogRepository.findWithFilters(searchAction, searchEntity, searchEmail, pageable);
    }
    
    public Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }
    
    public Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }
    
    public Page<AuditLog> getAuditLogsByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityType(entityType, pageable);
    }
    
    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return auditLogRepository.findByCreatedAtBetween(from, to, pageable);
    }
    
    public List<AuditLog> getEntityHistory(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
    
    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "N/A";
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
