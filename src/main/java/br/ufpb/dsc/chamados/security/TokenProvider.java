package br.ufpb.dsc.chamados.security;

import br.ufpb.dsc.chamados.domain.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Gerador e validador de tokens JWT.
 *
 * Utiliza a biblioteca Auth0 JWT para criar tokens assinados com HMAC256.
 * O token contém:
 * - subject: ID do usuário
 * - claim 'matricula': matrícula do usuário
 * - expiração: configurável em minutos
 */
@Setter
@Slf4j
@Component
public class TokenProvider {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-minutes:60}")
    private Integer expirationMinutes;

    private Algorithm algorithm;

    @PostConstruct
    public void setUp() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("app.jwt.secret não configurada! Configure a variável de ambiente JWT_SECRET.");
        }
        algorithm = Algorithm.HMAC256(secret.getBytes());
        log.info("TokenProvider inicializado com expiração de {} minutos", expirationMinutes);
    }

    /**
     * Gera token JWT para um usuário autenticado.
     *
     * @param usuario Entidade do usuário
     * @return String com o token JWT
     */
    public String generateToken(Usuario usuario) {
        try {
            return JWT.create()
                    .withSubject(usuario.getId().toString())           // ID do usuário
                    .withClaim("matricula", usuario.getMatricula())    // Matrícula
                    .withClaim("nomeCompleto", usuario.getNomeCompleto()) // Nome
                    .withIssuedAt(Instant.now())
                    .withExpiresAt(expirationToken())
                    .sign(algorithm)
                    .strip();
        } catch (JWTCreationException e) {
            log.error("Erro ao gerar token para usuário {}: {}", usuario.getId(), e.getMessage());
            throw new RuntimeException("Erro ao gerar token JWT", e);
        }
    }

    /**
     * Valida e decodifica um token JWT.
     *
     * @param token String do token
     * @return DecodedJWT com as claims do token
     * @throws RuntimeException se o token for inválido ou expirado
     */
    public DecodedJWT verifyToken(String token) {
        try {
            return JWT.require(algorithm)
                    .build()
                    .verify(token);
        } catch (JWTVerificationException e) {
            log.warn("Token inválido ou expirado: {}", e.getMessage());
            throw new RuntimeException("Token inválido ou expirado", e);
        }
    }

    /**
     * Extrai o ID do usuário do token.
     *
     * @param token String do token
     * @return ID do usuário (subject)
     */
    public Long getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = verifyToken(token);
            return Long.parseLong(jwt.getSubject());
        } catch (Exception e) {
            log.error("Erro ao extrair ID do token: {}", e.getMessage());
            throw new RuntimeException("Erro ao processar token", e);
        }
    }

    /**
     * Extrai a matrícula do usuário do token.
     */
    public String getMatriculaFromToken(String token) {
        DecodedJWT jwt = verifyToken(token);
        return jwt.getClaim("matricula").asString();
    }

    /**
     * Verifica se um token é válido.
     */
    public boolean isValidToken(String token) {
        try {
            verifyToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Instant expirationToken() {
        return LocalDateTime.now()
                .plusMinutes(expirationMinutes)
                .toInstant(ZoneOffset.of("-03:00"));
    }
}
