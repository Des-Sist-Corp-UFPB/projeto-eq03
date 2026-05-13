package br.ufpb.dsc.chamados.service;

import br.ufpb.dsc.chamados.domain.Usuario;
import br.ufpb.dsc.chamados.dto.UsuarioForm;
import br.ufpb.dsc.chamados.exception.UsuarioNaoEncontradoException;
import br.ufpb.dsc.chamados.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para UsuarioService.
 * 
 * @DataJpaTest apenas carrega a camada de persistência (JPA).
 * @Import(UsuarioService.class) importa o service para testes.
 * Usando TestContainers com PostgreSQL para testes de integração.
 */
@DataJpaTest
@Import(UsuarioService.class)
class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private UsuarioForm formularioValido;

    @BeforeEach
    void setUp() {
        formularioValido = new UsuarioForm(
                "mat001",
                "João Silva",
                "senha123",
                "joao@example.com",
                true
        );
    }

    @Test
    void testSalvarUsuario() {
        Usuario usuario = usuarioService.salvar(formularioValido);

        assertNotNull(usuario);
        assertNotNull(usuario.getId());
        assertEquals("mat001", usuario.getMatricula());
        assertEquals("João Silva", usuario.getNomeCompleto());
        assertTrue(usuario.getAtivo());
    }

    @Test
    void testSalvarUsuarioDuplicado() {
        usuarioService.salvar(formularioValido);

        assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.salvar(formularioValido);
        });
    }

    @Test
    void testListarTodos() {
        usuarioService.salvar(formularioValido);
        usuarioService.salvar(new UsuarioForm("mat002", "Maria Santos", "senha456", "maria@example.com", true));

        var usuarios = usuarioService.listarTodos();
        assertEquals(2, usuarios.size());
    }

    @Test
    void testBuscarPorId() {
        Usuario usuarioSalvo = usuarioService.salvar(formularioValido);

        Usuario usuarioBuscado = usuarioService.buscarPorId(usuarioSalvo.getId());
        assertEquals(usuarioSalvo.getId(), usuarioBuscado.getId());
    }

    @Test
    void testBuscarPorIdNaoEncontrado() {
        assertThrows(UsuarioNaoEncontradoException.class, () -> {
            usuarioService.buscarPorId(999L);
        });
    }

    @Test
    void testAtualizarUsuario() {
        Usuario usuarioSalvo = usuarioService.salvar(formularioValido);

        UsuarioForm formularioAtualizado = new UsuarioForm(
                "mat001",
                "João Silva Atualizado",
                "novaSenha123",
                "joao.novo@example.com",
                true
        );

        Usuario usuarioAtualizado = usuarioService.atualizar(usuarioSalvo.getId(), formularioAtualizado);

        assertEquals("João Silva Atualizado", usuarioAtualizado.getNomeCompleto());
        assertEquals("joao.novo@example.com", usuarioAtualizado.getEmail());
    }

    @Test
    void testExcluirUsuario() {
        Usuario usuarioSalvo = usuarioService.salvar(formularioValido);

        usuarioService.excluir(usuarioSalvo.getId());

        assertThrows(UsuarioNaoEncontradoException.class, () -> {
            usuarioService.buscarPorId(usuarioSalvo.getId());
        });
    }
}
