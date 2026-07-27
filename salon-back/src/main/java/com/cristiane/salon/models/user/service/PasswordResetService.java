package com.cristiane.salon.models.user.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.integrations.email.service.EmailService;
import com.cristiane.salon.models.audit.AuditLogService;
import com.cristiane.salon.models.user.entity.PasswordResetToken;
import com.cristiane.salon.models.user.entity.User;
import com.cristiane.salon.models.user.repository.PasswordResetTokenRepository;
import com.cristiane.salon.models.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Fluxo "esqueci minha senha": gera um token de uso único enviado por e-mail e, na segunda
 * etapa, troca a senha do usuário se o token ainda for válido. Mesmo padrão de hash de
 * {@link com.cristiane.salon.models.ai.service.McpTokenService} — só o hash é persistido.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_RANDOM_BYTES = 32;
    private static final int TOKEN_TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    /**
     * Não revela se o e-mail existe na base — do ponto de vista de quem chamou, o resultado
     * é sempre "se existir, você recebe um e-mail", evitando enumeração de contas.
     */
    @Transactional
    public void requestReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }
        User user = userOpt.get();

        // Só o token mais recente deve funcionar — invalida qualquer solicitação anterior.
        tokenRepository.deleteByUserId(user.getId());

        String rawToken = randomUrlSafeToken();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(TOKEN_TTL_MINUTES, ChronoUnit.MINUTES))
                .build();
        tokenRepository.save(token);

        emailService.sendPasswordResetEmail(user, rawToken);

        auditLogService.logAction(
                user.getId(),
                user.getEmail(),
                "PASSWORD_RESET_REQUESTED",
                "User",
                user.getId(),
                "Solicitação de redefinição de senha para: " + user.getEmail(),
                "SUCCESS"
        );
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .filter(PasswordResetToken::isValid)
                .orElseThrow(() -> new BadRequestException("Link de redefinição inválido ou expirado."));

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);

        auditLogService.logAction(
                user.getId(),
                user.getEmail(),
                "PASSWORD_RESET_COMPLETED",
                "User",
                user.getId(),
                "Senha redefinida com sucesso para: " + user.getEmail(),
                "SUCCESS"
        );
    }

    private String randomUrlSafeToken() {
        byte[] bytes = new byte[TOKEN_RANDOM_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new BadRequestException("Falha ao processar token");
        }
    }
}
