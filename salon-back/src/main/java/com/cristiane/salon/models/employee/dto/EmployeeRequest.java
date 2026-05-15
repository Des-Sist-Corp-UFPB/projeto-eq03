package com.cristiane.salon.models.employee.dto;

import jakarta.validation.constraints.NotNull;

public record EmployeeRequest(
        @NotNull(message = "O ID do usuário é obrigatório")
        Long userId,

        String bio
) {}
