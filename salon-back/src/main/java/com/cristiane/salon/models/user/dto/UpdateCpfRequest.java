package com.cristiane.salon.models.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateCpfRequest(
        @NotBlank(message = "O CPF é obrigatório")
        @Pattern(regexp = "^\\d{11}$", message = "O CPF deve conter exatamente 11 dígitos numéricos (sem pontos ou traços)")
        String cpf
) {}
