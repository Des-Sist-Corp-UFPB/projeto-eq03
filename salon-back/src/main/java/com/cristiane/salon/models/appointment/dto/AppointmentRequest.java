package com.cristiane.salon.models.appointment.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record AppointmentRequest(
        @NotNull(message = "O funcionário é obrigatório")
        Long employeeId,

        @NotNull(message = "O serviço é obrigatório")
        Long serviceId,

        @NotNull(message = "A data e hora são obrigatórias")
        LocalDateTime scheduledAt,

        Long clientId
) {}
