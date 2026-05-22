package com.cristiane.salon.models.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    Page<AuditLog> findAll(Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:action IS NULL OR " +
           "  (:action = 'CREATE' AND a.action LIKE 'POST %') OR " +
           "  (:action = 'UPDATE' AND (a.action LIKE 'PUT %' OR a.action LIKE 'PATCH %')) OR " +
           "  (:action = 'DELETE' AND a.action LIKE 'DELETE %') OR " +
           "  (a.action = :action) OR (a.action LIKE CONCAT(:action, ' %'))" +
           ") AND " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:userEmail IS NULL OR LOWER(a.userEmail) LIKE :userEmail)")
    Page<AuditLog> findWithFilters(
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("userEmail") String userEmail,
            Pageable pageable);
    
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
    
    Page<AuditLog> findByAction(String action, Pageable pageable);
    
    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);
    
    Page<AuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
    
    List<AuditLog> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime from, LocalDateTime to);
    
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
