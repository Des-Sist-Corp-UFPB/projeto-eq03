package com.cristiane.salon.models.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SalonServiceRequest(
        @NotBlank(message = "O nome é obrigatório")
        String name,

        String description,

        /** Opcional: exibido como &quot;a partir de&quot; no site */
        @Min(value = 0, message = "O preço não pode ser negativo")
        BigDecimal price,

        @NotNull(message = "A duração é obrigatória")
        @Min(value = 1, message = "A duração mínima é 1 minuto")
        Integer durationMin,

        Boolean active
) {}
