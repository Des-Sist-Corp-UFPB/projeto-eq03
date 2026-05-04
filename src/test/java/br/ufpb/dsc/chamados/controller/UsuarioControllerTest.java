package br.ufpb.dsc.chamados.controller;

import br.ufpb.dsc.chamados.domain.Usuario;
import br.ufpb.dsc.chamados.dto.UsuarioForm;
import br.ufpb.dsc.chamados.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para UsuarioController.
 * 
 * @SpringBootTest carrega todo o contexto da aplicação.
 * @AutoConfigureMockMvc fornece MockMvc para testar endpoints HTTP.
 * @WithMockUser simula um usuário autenticado.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testListarUsuarios() throws Exception {
        Usuario usuario1 = new Usuario("mat001", "João Silva", "senha123", "joao@example.com");
        usuario1.setId(1L);

        when(usuarioService.listarTodos()).thenReturn(Arrays.asList(usuario1));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/lista"))
                .andExpect(model().attributeExists("usuarios"));

        verify(usuarioService, times(1)).listarTodos();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testNovoFormulario() throws Exception {
        mockMvc.perform(get("/usuarios/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/fragments/form"))
                .andExpect(model().attributeExists("usuarioForm"));
    }

    @Test
    void testAcessarSemAutenticacao() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
