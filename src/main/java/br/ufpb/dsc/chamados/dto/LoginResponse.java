package br.ufpb.dsc.chamados.dto;

/**
 * DTO para resposta de login.
 * Retorna o token JWT gerado.
 */
public record LoginResponse(
        String token,
        String matricula,
        String nomeCompleto
) {
}
