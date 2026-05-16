package com.cristiane.salon.models.appointment.repository;

import com.cristiane.salon.models.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    
    List<Appointment> findByClientId(Long clientId);
    
    @Query("SELECT a FROM Appointment a WHERE a.employee.id = :employeeId AND a.scheduledAt >= :startOfDay AND a.scheduledAt < :endOfDay AND a.scheduledAt IS NOT NULL AND a.status NOT IN ('CANCELLED', 'DECLINED')")
    List<Appointment> findActiveAppointmentsByEmployeeAndDate(
            @Param("employeeId") Long employeeId, 
            @Param("startOfDay") LocalDateTime startOfDay, 
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
