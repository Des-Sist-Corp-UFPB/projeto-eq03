package com.cristiane.salon.models.employee.dto;

import com.cristiane.salon.models.employee.entity.RemunerationType;
import com.cristiane.salon.models.employee.entity.CommissionScope;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record EmployeeRequest(
        @NotNull(message = "O ID do usuário é obrigatório")
        Long userId,

        @Size(max = 1000, message = "A biografia deve ter no máximo 1000 caracteres")
        String bio,

        RemunerationType remunerationType,

        CommissionScope commissionScope,

        BigDecimal remunerationValue,

        BigDecimal commissionValue
) {}
