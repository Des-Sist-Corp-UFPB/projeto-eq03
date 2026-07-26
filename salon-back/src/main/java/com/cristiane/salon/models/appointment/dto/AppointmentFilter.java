package com.cristiane.salon.models.appointment.dto;

import com.cristiane.salon.models.appointment.enums.AppointmentStatus;
import com.cristiane.salon.models.appointment.enums.PaymentStatus;

import java.time.LocalDate;

public record AppointmentFilter(
    AppointmentStatus status,
    PaymentStatus paymentStatus,
    Long employeeId,
    Long clientId,
    /** Busca parcial (case-insensitive) pelo nome do cliente. */
    String clientName,
    /** Início/fim do período — compara pela mesma cadeia de fallback de data usada nos
     *  relatórios (scheduledAt &gt; preferredDate &gt; createdAt), já que agendamentos
     *  PENDING/REQUESTED normalmente ainda não têm scheduledAt definido. */
    LocalDate startDate,
    LocalDate endDate
) {}
