package com.cristiane.salon.models.appointment.repository;

import com.cristiane.salon.models.appointment.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    List<Appointment> findByClientId(Long clientId);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.client.id = :clientId")
    Long countByClientId(@Param("clientId") Long clientId);

    @Query("SELECT MAX(a.scheduledAt) FROM Appointment a WHERE a.client.id = :clientId")
    LocalDateTime findLastAppointmentDateByClientId(@Param("clientId") Long clientId);

    @Query("SELECT a FROM Appointment a WHERE a.employee.id = :employeeId AND a.scheduledAt >= :startOfDay AND a.scheduledAt < :endOfDay AND a.scheduledAt IS NOT NULL AND a.status NOT IN ('CANCELLED', 'DECLINED')")
    List<Appointment> findActiveAppointmentsByEmployeeAndDate(
            @Param("employeeId") Long employeeId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    @Query("SELECT a FROM Appointment a WHERE a.employee.id = :employeeId "
            + "AND (:from IS NULL OR a.scheduledAt >= :from) "
            + "AND (:to IS NULL OR a.scheduledAt <= :to)")
    Page<Appointment> findByEmployeeIdForFinancialHistory(
            @Param("employeeId") Long employeeId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
