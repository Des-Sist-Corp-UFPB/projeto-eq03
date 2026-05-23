package com.cristiane.salon.models.cashflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CashFlowRequest(
        @NotBlank(message = "O tipo é obrigatório (INCOME ou EXPENSE)")
        String type,

        @NotNull(message = "O valor é obrigatório")
        @Min(value = 0, message = "O valor não pode ser negativo")
        BigDecimal amount,

        @NotBlank(message = "A descrição é obrigatória")
        String description,

        @NotNull(message = "A data é obrigatória")
        LocalDate date,
        
        Long appointmentId,

        List<CashFlowItemRequest> items
) {}
