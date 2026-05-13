package br.ufpb.dsc.chamados.exception;

public class UsuarioNaoEncontradoException extends RuntimeException {
    public UsuarioNaoEncontradoException(Long id) {
        super("Usuário com ID " + id + " não encontrado");
    }

    public UsuarioNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
