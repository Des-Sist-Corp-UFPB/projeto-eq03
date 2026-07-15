package com.cristiane.salon.models.ai.repository;

import com.cristiane.salon.models.ai.entity.AiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long> {

    @Query("SELECT COUNT(c) FROM AiCallLog c WHERE c.success = true AND c.createdAt >= :since")
    long countSuccessfulSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(c) FROM AiCallLog c WHERE c.success = true AND c.callerType = :callerType AND c.callerId = :callerId AND c.createdAt >= :since")
    long countSuccessfulByCallerSince(@Param("callerType") String callerType, @Param("callerId") String callerId, @Param("since") LocalDateTime since);
}
