package com.cristiane.salon.models.appointment.dto;

import com.cristiane.salon.models.appointment.enums.AppointmentStatus;
import com.cristiane.salon.models.appointment.enums.PaymentStatus;

public record AppointmentFilter(
    AppointmentStatus status,
    PaymentStatus paymentStatus,
    Long employeeId,
    Long clientId
) {}
