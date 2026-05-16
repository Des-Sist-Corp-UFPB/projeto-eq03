package com.cristiane.salon.models.appointment.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ConfirmAppointmentRequest(
        @NotNull(message = "Informe data e horário confirmados")
        LocalDateTime scheduledAt
) {}
