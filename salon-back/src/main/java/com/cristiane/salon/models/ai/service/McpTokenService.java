package com.cristiane.salon.models.ai.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.ai.dto.McpTokenCreateRequest;
import com.cristiane.salon.models.ai.dto.McpTokenGeneratedResponse;
import com.cristiane.salon.models.ai.dto.McpTokenResponse;
import com.cristiane.salon.models.ai.entity.McpAccessToken;
import com.cristiane.salon.models.ai.repository.McpAccessTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Gera, lista, revoga e valida tokens de acesso ao servidor MCP. Múltiplos tokens nomeados
 * (não um segredo único compartilhado) — cada integração/pessoa tem o próprio, revogável sem
 * afetar as demais.
 */
@Service
@RequiredArgsConstructor
public class McpTokenService {

    private static final String TOKEN_PREFIX = "mcp_";
    private static final int TOKEN_RANDOM_BYTES = 32;

    private final McpAccessTokenRepository repository;

    @Transactional
    public McpTokenGeneratedResponse generate(McpTokenCreateRequest request) {
        String rawToken = TOKEN_PREFIX + randomUrlSafeToken();

        McpAccessToken token = McpAccessToken.builder()
                .name(request.name())
                .tokenHash(hash(rawToken))
                .createdBy(currentUserEmail())
                .createdAt(LocalDateTime.now())
                .expiresAt(request.expiresInDays() != null ? LocalDateTime.now().plusDays(request.expiresInDays()) : null)
                .revoked(false)
                .build();

        McpAccessToken saved = repository.save(token);
        return new McpTokenGeneratedResponse(McpTokenResponse.fromEntity(saved), rawToken);
    }

    @Transactional(readOnly = true)
    public List<McpTokenResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(McpTokenResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void revoke(Long id) {
        McpAccessToken token = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Token MCP não encontrado: " + id));
        token.setRevoked(true);
        repository.save(token);
    }

    /** Valida um token recebido (ex.: no header Authorization do servidor MCP) e atualiza o último uso. */
    @Transactional
    public Optional<McpAccessToken> validateAndTouch(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        Optional<McpAccessToken> found = repository.findByTokenHash(hash(rawToken));
        if (found.isEmpty() || !found.get().isValid()) {
            return Optional.empty();
        }
        McpAccessToken token = found.get();
        token.setLastUsedAt(LocalDateTime.now());
        repository.save(token);
        return Optional.of(token);
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

    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
