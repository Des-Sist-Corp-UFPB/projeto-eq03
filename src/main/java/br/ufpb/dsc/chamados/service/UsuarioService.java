package br.ufpb.dsc.chamados.service;

import br.ufpb.dsc.chamados.domain.Usuario;
import br.ufpb.dsc.chamados.dto.UsuarioForm;
import br.ufpb.dsc.chamados.exception.UsuarioNaoEncontradoException;
import br.ufpb.dsc.chamados.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario salvar(UsuarioForm form) {
        // Verificar se matrícula já existe
        if (usuarioRepository.findByMatricula(form.matricula()).isPresent()) {
            throw new IllegalArgumentException("Matrícula '" + form.matricula() + "' já existe");
        }

        Usuario usuario = new Usuario(
                form.matricula(),
                form.nomeCompleto(),
                passwordEncoder.encode(form.senha()),  // Criptografa a senha
                form.email()
        );

        return usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    @Transactional
    public Usuario atualizar(Long id, UsuarioForm form) {
        Usuario usuario = buscarPorId(id);

        // Verificar se matrícula já existe em outro usuário
        if (!usuario.getMatricula().equals(form.matricula())) {
            if (usuarioRepository.findByMatricula(form.matricula()).isPresent()) {
                throw new IllegalArgumentException("Matrícula '" + form.matricula() + "' já existe");
            }
        }

        usuario.setMatricula(form.matricula());
        usuario.setNomeCompleto(form.nomeCompleto());
        usuario.setSenha(passwordEncoder.encode(form.senha()));  // Criptografa a nova senha
        usuario.setEmail(form.email());
        if (form.ativo() != null) {
            usuario.setAtivo(form.ativo());
        }

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void inativar(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void excluir(Long id) {
        buscarPorId(id); // Valida se existe
        usuarioRepository.deleteById(id);
    }
}
