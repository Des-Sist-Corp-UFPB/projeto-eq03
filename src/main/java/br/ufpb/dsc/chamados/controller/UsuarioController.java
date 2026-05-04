package br.ufpb.dsc.chamados.controller;

import br.ufpb.dsc.chamados.domain.Usuario;
import br.ufpb.dsc.chamados.dto.UsuarioForm;
import br.ufpb.dsc.chamados.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    /**
     * GET /usuarios - Página de listagem
     * 
     * Se for requisição HTMX (HX-Request header), retorna apenas a tabela (fragmento).
     * Se for navegação normal, retorna a página completa com layout.
     */
    @GetMapping
    public String listar(Model model, HttpServletRequest request) {
        List<Usuario> usuarios = usuarioService.listarTodos();
        model.addAttribute("usuarios", usuarios);
        
        // Detecta se é requisição HTMX pelo header HX-Request
        boolean isHtmxRequest = "true".equals(request.getHeader("HX-Request"));
        
        if (isHtmxRequest) {
            // HTMX quer apenas a tabela (fragmento)
            return "usuarios/fragments/tabela";
        } else {
            // Navegação normal quer a página completa com layout
            return "usuarios/lista";
        }
    }

    /**
     * GET /usuarios/novo - Fragmento do formulário de criação
     */
    @GetMapping("/novo")
    public String novoFormulario(Model model) {
        model.addAttribute("usuarioForm", new UsuarioForm(null, null, null, null, true));
        return "usuarios/fragments/form";
    }

    /**
     * POST /usuarios - Salva novo usuário
     */
    @PostMapping
    public String salvar(@Valid @ModelAttribute UsuarioForm usuarioForm, 
                        BindingResult result, 
                        Model model) {
        if (result.hasErrors()) {
            return "usuarios/fragments/form";
        }

        try {
            usuarioService.salvar(usuarioForm);
        } catch (IllegalArgumentException e) {
            result.rejectValue("matricula", "error.matricula", e.getMessage());
            return "usuarios/fragments/form";
        }

        List<Usuario> usuarios = usuarioService.listarTodos();
        model.addAttribute("usuarios", usuarios);
        return "usuarios/fragments/tabela";
    }

    /**
     * GET /usuarios/{id}/editar - Fragmento do formulário de edição
     */
    @GetMapping("/{id}/editar")
    public String editarFormulario(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.buscarPorId(id);
        UsuarioForm form = new UsuarioForm(
                usuario.getMatricula(),
                usuario.getNomeCompleto(),
                usuario.getSenha(),
                usuario.getEmail(),
                usuario.getAtivo()
        );
        model.addAttribute("usuarioForm", form);
        model.addAttribute("usuarioId", id);
        return "usuarios/fragments/form";
    }

    /**
     * PUT /usuarios/{id} - Atualiza usuário
     */
    @PutMapping("/{id}")
    public String atualizar(@PathVariable Long id,
                           @Valid @ModelAttribute UsuarioForm usuarioForm,
                           BindingResult result,
                           Model model) {
        if (result.hasErrors()) {
            model.addAttribute("usuarioId", id);
            return "usuarios/fragments/form";
        }

        try {
            usuarioService.atualizar(id, usuarioForm);
        } catch (IllegalArgumentException e) {
            result.rejectValue("matricula", "error.matricula", e.getMessage());
            model.addAttribute("usuarioId", id);
            return "usuarios/fragments/form";
        }

        List<Usuario> usuarios = usuarioService.listarTodos();
        model.addAttribute("usuarios", usuarios);
        return "usuarios/fragments/tabela";
    }

    /**
     * DELETE /usuarios/{id} - Deleta (ou inativa) o usuário
     */
    @DeleteMapping("/{id}")
    public String excluir(@PathVariable Long id, Model model) {
        usuarioService.excluir(id);
        List<Usuario> usuarios = usuarioService.listarTodos();
        model.addAttribute("usuarios", usuarios);
        return "usuarios/fragments/tabela";
    }
}
