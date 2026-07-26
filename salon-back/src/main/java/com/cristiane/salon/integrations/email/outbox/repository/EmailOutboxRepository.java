package com.cristiane.salon.integrations.email.outbox.repository;

import com.cristiane.salon.integrations.email.outbox.entity.EmailOutboxEntry;
import com.cristiane.salon.integrations.email.outbox.enums.EmailOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EmailOutboxRepository extends JpaRepository<EmailOutboxEntry, Long>,
        JpaSpecificationExecutor<EmailOutboxEntry> {

    List<EmailOutboxEntry> findByStatusAndNextRetryAtBefore(EmailOutboxStatus status, LocalDateTime now);

    @Modifying
    @Query("DELETE FROM EmailOutboxEntry e WHERE e.status = :status AND e.updatedAt < :cutoff")
    int deleteByStatusAndUpdatedAtBefore(@Param("status") EmailOutboxStatus status, @Param("cutoff") LocalDateTime cutoff);
}
