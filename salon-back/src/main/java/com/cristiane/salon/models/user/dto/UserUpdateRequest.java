package com.cristiane.salon.models.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
        String name,

        @Email(message = "O formato do email é inválido")
        String email,

        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String password,

        @Size(max = 20, message = "O telefone não pode exceder 20 caracteres")
        String phone,

        @Pattern(regexp = "^\\d{11}$", message = "O CPF deve conter exatamente 11 dígitos numéricos (sem pontos ou traços)")
        String cpf,

        Boolean active,

        Long roleId
) {}

