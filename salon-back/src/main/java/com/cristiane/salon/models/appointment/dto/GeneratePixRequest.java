package com.cristiane.salon.models.appointment.dto;

import jakarta.validation.constraints.NotNull;

public record GeneratePixRequest(
    @NotNull(message = "useSavedCpf é obrigatório")
    Boolean useSavedCpf,
    
    String cpf
) {}
