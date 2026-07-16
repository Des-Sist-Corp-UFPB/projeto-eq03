package com.cristiane.salon.models.report.dto;

import com.cristiane.salon.models.appointment.entity.Appointment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AppointmentFinancialResponse(
        Long id,
        LocalDateTime scheduledAt,
        LocalDate preferredDate,
        String serviceName,
        BigDecimal price,
        String status,
        String paymentStatus
) {
    public static AppointmentFinancialResponse fromEntity(Appointment appointment) {
        return new AppointmentFinancialResponse(
                appointment.getId(),
                appointment.getScheduledAt(),
                appointment.getPreferredDate(),
                appointment.getSalonService().getName(),
                appointment.getSalonService().getPrice(),
                appointment.getStatus().name(),
                appointment.getPaymentStatus() != null ? appointment.getPaymentStatus().name() : null
        );
    }
}
