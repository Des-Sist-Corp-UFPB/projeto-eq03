package com.cristiane.salon.models.appointment.dto;

import com.cristiane.salon.models.appointment.entity.Appointment;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long clientId,
        String clientName,
        Long employeeId,
        String employeeName,
        Long serviceId,
        String serviceName,
        LocalDateTime scheduledAt,
        LocalDate preferredDate,
        String clientNotes,
        String status,
        String paymentStatus,
        Long paymentId,
        String pixQrCode,
        Boolean clientHasSavedCpf,
        String clientCpfMasked
) {
    public AppointmentResponse(
            Long id,
            Long clientId,
            String clientName,
            Long employeeId,
            String employeeName,
            Long serviceId,
            String serviceName,
            LocalDateTime scheduledAt,
            LocalDate preferredDate,
            String clientNotes,
            String status
    ) {
        this(id, clientId, clientName, employeeId, employeeName, serviceId, serviceName,
                scheduledAt, preferredDate, clientNotes, status, null, null, null, false, "");
    }

    public AppointmentResponse(
            Long id,
            Long clientId,
            String clientName,
            Long employeeId,
            String employeeName,
            Long serviceId,
            String serviceName,
            LocalDateTime scheduledAt,
            LocalDate preferredDate,
            String clientNotes,
            String status,
            String paymentStatus,
            Long paymentId,
            String pixQrCode
    ) {
        this(id, clientId, clientName, employeeId, employeeName, serviceId, serviceName,
                scheduledAt, preferredDate, clientNotes, status, paymentStatus, paymentId, pixQrCode, false, "");
    }

    public static AppointmentResponse fromEntity(Appointment appointment) {
        String rawCpf = appointment.getClient().getCpf();
        boolean hasSavedCpf = rawCpf != null && !rawCpf.isBlank();
        String maskedCpf = "";
        if (hasSavedCpf) {
            String clean = rawCpf.replaceAll("\\D", "");
            if (clean.length() == 11) {
                maskedCpf = "***.***." + clean.substring(6, 9) + "-";
            } else {
                maskedCpf = rawCpf;
            }
        }

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getClient().getId(),
                appointment.getClient().getName(),
                appointment.getEmployee().getId(),
                appointment.getEmployee().getUser().getName(),
                appointment.getSalonService().getId(),
                appointment.getSalonService().getName(),
                appointment.getScheduledAt(),
                appointment.getPreferredDate(),
                appointment.getClientNotes(),
                appointment.getStatus().name(),
                appointment.getPaymentStatus() != null ? appointment.getPaymentStatus().name() : null,
                appointment.getPaymentId(),
                appointment.getPixQrCode(),
                hasSavedCpf,
                maskedCpf
        );
    }
}
