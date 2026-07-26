package com.cristiane.salon.models.staff.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GenerateStaffPixRequest(
        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "Valor inválido")
        BigDecimal amount,

        @Size(max = 25, message = "A descrição deve ter no máximo 25 caracteres")
        String description
) {
}
