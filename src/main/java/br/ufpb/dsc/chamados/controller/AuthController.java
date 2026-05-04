package br.ufpb.dsc.chamados.controller;

import br.ufpb.dsc.chamados.domain.Usuario;
import br.ufpb.dsc.chamados.dto.LoginRequest;
import br.ufpb.dsc.chamados.dto.LoginResponse;
import br.ufpb.dsc.chamados.repository.UsuarioRepository;
import br.ufpb.dsc.chamados.security.TokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Controller responsável por autenticação e login.
 *
 * Endpoints:
 * - GET /login - Retorna página de login (HTML)
 * - POST /api/auth/login - Autentica com JWT (JSON)
 */
@Slf4j
@Controller
@RequestMapping
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * GET / - Redireciona para /login.
     * Quando usuário não autenticado tenta acessar a raiz, vai para login.
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    /**
     * GET /login - Página de login com formulário HTML.
     * Acessível sem autenticação.
     */
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    /**
     * POST /api/auth/login - Autentica usuário e retorna JWT.
     *
     * Request:
     * {
     *   "matricula": "mat001",
     *   "senha": "senha123"
     * }
     *
     * Response (sucesso):
     * {
     *   "token": "eyJhbGc...",
     *   "matricula": "mat001",
     *   "nomeCompleto": "João Silva"
     * }
     *
     * Response (erro):
     * HTTP 401 - Credenciais inválidas
     */
    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            log.info("Tentativa de login para matrícula: {}", loginRequest.matricula());

            // 1. Autentica as credenciais
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.matricula(),
                            loginRequest.senha()
                    )
            );

            log.info("Autenticação bem-sucedida para: {}", loginRequest.matricula());

            // 2. Busca o usuário para gerar token com todas as informações
            Usuario usuario = usuarioRepository.findByMatricula(loginRequest.matricula())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            // 3. Gera token JWT
            String token = tokenProvider.generateToken(usuario);

            log.info("Token gerado com sucesso para: {}", loginRequest.matricula());

            // 4. Retorna resposta com token
            LoginResponse response = new LoginResponse(
                    token,
                    usuario.getMatricula(),
                    usuario.getNomeCompleto()
            );

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            log.warn("Falha de autenticação para matrícula: {} - Motivo: {}", 
                    loginRequest.matricula(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(null, null, "Credenciais inválidas"));
        } catch (Exception e) {
            log.error("Erro durante login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new LoginResponse(null, null, "Erro no servidor"));
        }
    }
}
