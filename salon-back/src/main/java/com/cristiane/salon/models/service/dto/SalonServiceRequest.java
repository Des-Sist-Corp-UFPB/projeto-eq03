package com.cristiane.salon.models.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SalonServiceRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
        String name,

        String description,

        @NotNull(message = "O preço é obrigatório")
        @Min(value = 0, message = "O preço não pode ser negativo")
        BigDecimal price,

        @NotNull(message = "A duração é obrigatória")
        @Min(value = 1, message = "A duração mínima é 1 minuto")
        Integer durationMin,

        Boolean active
) {}
