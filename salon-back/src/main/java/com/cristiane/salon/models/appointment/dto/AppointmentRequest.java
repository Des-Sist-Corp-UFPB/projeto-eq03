package com.cristiane.salon.models.appointment.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AppointmentRequest(
        @NotNull(message = "O funcionário é obrigatório")
        Long employeeId,

        @NotNull(message = "O serviço é obrigatório")
        Long serviceId,

        /**
         * Obrigatório apenas no fluxo administrativo (agendamento com horário definido).
         * No fluxo do cliente deve ser omitido/null — o salão confirma o horário depois.
         */
        LocalDateTime scheduledAt,

        /** Cliente indica dia preferido (opcional). */
        LocalDate preferredDate,

        /** Observações do cliente (opcional). */
        String clientNotes,

        /** Preenchido apenas quando admin/gerente agenda para um cliente. */
        Long clientId
) {
}
