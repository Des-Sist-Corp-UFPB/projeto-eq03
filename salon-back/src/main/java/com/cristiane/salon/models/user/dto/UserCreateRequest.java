package com.cristiane.salon.models.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
        String name,

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "O formato do email é inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        @Pattern(regexp = "^(?=.*\\d).*$", message = "A senha deve conter pelo menos um número")
        String password,

        @Size(max = 20, message = "O telefone não pode exceder 20 caracteres")
        String phone,

        Boolean active,
        
        @NotNull(message = "A role / papel é obrigatória")
        Long roleId
) {}