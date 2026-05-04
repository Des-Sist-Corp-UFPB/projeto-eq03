package br.ufpb.dsc.chamados.dto;

/**
 * DTO para requisição de login.
 * Recebe matrícula e senha do usuário.
 */
public record LoginRequest(
        String matricula,
        String senha
) {
}
