package br.ufpb.dsc.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UsuarioForm(
        @NotBlank(message = "Matrícula é obrigatória")
        String matricula,

        @NotBlank(message = "Nome completo é obrigatório")
        String nomeCompleto,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter pelo menos 6 caracteres")
        String senha,

        @Email(message = "Email deve ser válido")
        String email,

        Boolean ativo
) {
}
