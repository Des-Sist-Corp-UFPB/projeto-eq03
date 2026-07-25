package com.cristiane.salon.models.user.controller;

import com.cristiane.salon.models.user.dto.ForgotPasswordRequest;
import com.cristiane.salon.models.user.dto.LoginRequest;
import com.cristiane.salon.models.user.dto.RefreshTokenRequest;
import com.cristiane.salon.models.user.dto.RegisterRequest;
import com.cristiane.salon.models.user.dto.ResetPasswordRequest;
import com.cristiane.salon.models.user.dto.TokenResponse;
import com.cristiane.salon.models.user.dto.UserProfileResponse;
import com.cristiane.salon.models.user.service.AuthService;
import com.cristiane.salon.models.user.service.PasswordResetService;
import com.cristiane.salon.models.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints para login, registro e refresh token")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    @Operation(summary = "Realiza o login do usuário")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Registra um novo cliente")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o token de acesso")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    /**
     * Sempre responde 200, exista ou não o e-mail — evita que alguém descubra quais
     * e-mails têm conta cadastrada só testando esse endpoint (enumeração de contas).
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "Solicita redefinição de senha por e-mail")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefine a senha usando o token recebido por e-mail")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    /**
     * Retorna o perfil completo do usuário autenticado com suas permissões
     * lidas diretamente do banco de dados. Chamado pelo frontend no startup do app
     * para popular o estado de autorização (CanI) sem inflar o JWT.
     *
     * Este endpoint é protegido: requer um JWT válido (qualquer role autenticado pode acessar).
     */
    @GetMapping("/me")
    @Operation(summary = "Retorna o perfil e permissões do usuário autenticado")
    public ResponseEntity<UserProfileResponse> me() {
        return ResponseEntity.ok(userService.getMyProfile());
    }
}

