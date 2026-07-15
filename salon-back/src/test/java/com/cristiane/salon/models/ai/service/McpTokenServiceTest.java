package com.cristiane.salon.models.ai.service;

import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.ai.dto.McpTokenCreateRequest;
import com.cristiane.salon.models.ai.dto.McpTokenGeneratedResponse;
import com.cristiane.salon.models.ai.entity.McpAccessToken;
import com.cristiane.salon.models.ai.repository.McpAccessTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpTokenServiceTest {

    @Mock
    private McpAccessTokenRepository repository;

    private McpTokenService service;

    @BeforeEach
    void setUp() {
        service = new McpTokenService(repository);
    }

    @Test
    void generate_returnsRawTokenOnceAndPersistsOnlyTheHash() {
        when(repository.save(any(McpAccessToken.class))).thenAnswer(inv -> {
            McpAccessToken t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        McpTokenGeneratedResponse response = service.generate(new McpTokenCreateRequest("Claude Desktop", 90));

        assertThat(response.rawValue()).startsWith("mcp_");
        assertThat(response.token().name()).isEqualTo("Claude Desktop");

        ArgumentCaptor<McpAccessToken> captor = ArgumentCaptor.forClass(McpAccessToken.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(response.rawValue());
        assertThat(captor.getValue().getTokenHash()).doesNotContain(response.rawValue());
        assertThat(captor.getValue().getExpiresAt()).isAfter(LocalDateTime.now().plusDays(89));
    }

    @Test
    void generate_withoutExpiration_hasNullExpiresAt() {
        when(repository.save(any(McpAccessToken.class))).thenAnswer(inv -> inv.getArgument(0));

        McpTokenGeneratedResponse response = service.generate(new McpTokenCreateRequest("Sem validade", null));

        assertThat(response.token().expiresAt()).isNull();
    }

    @Test
    void revoke_setsRevokedFlag() {
        McpAccessToken token = McpAccessToken.builder().id(1L).revoked(false).build();
        when(repository.findById(1L)).thenReturn(Optional.of(token));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.revoke(1L);

        assertThat(token.getRevoked()).isTrue();
    }

    @Test
    void revoke_whenTokenDoesNotExist_throwsResourceNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void validateAndTouch_withValidToken_updatesLastUsedAtAndReturnsIt() {
        McpTokenGeneratedResponse generated = generateWithCapture();

        McpAccessToken stored = McpAccessToken.builder()
                .id(1L).tokenHash(hashOf(generated.rawValue())).revoked(false).build();
        when(repository.findByTokenHash(hashOf(generated.rawValue()))).thenReturn(Optional.of(stored));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<McpAccessToken> result = service.validateAndTouch(generated.rawValue());

        assertThat(result).isPresent();
        assertThat(stored.getLastUsedAt()).isNotNull();
    }

    @Test
    void validateAndTouch_withRevokedToken_returnsEmpty() {
        McpTokenGeneratedResponse generated = generateWithCapture();
        McpAccessToken stored = McpAccessToken.builder()
                .id(1L).tokenHash(hashOf(generated.rawValue())).revoked(true).build();
        when(repository.findByTokenHash(hashOf(generated.rawValue()))).thenReturn(Optional.of(stored));

        assertThat(service.validateAndTouch(generated.rawValue())).isEmpty();
    }

    @Test
    void validateAndTouch_withExpiredToken_returnsEmpty() {
        McpTokenGeneratedResponse generated = generateWithCapture();
        McpAccessToken stored = McpAccessToken.builder()
                .id(1L).tokenHash(hashOf(generated.rawValue())).revoked(false)
                .expiresAt(LocalDateTime.now().minusDays(1)).build();
        when(repository.findByTokenHash(hashOf(generated.rawValue()))).thenReturn(Optional.of(stored));

        assertThat(service.validateAndTouch(generated.rawValue())).isEmpty();
    }

    @Test
    void validateAndTouch_withUnknownToken_returnsEmpty() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThat(service.validateAndTouch("mcp_does-not-exist")).isEmpty();
    }

    @Test
    void validateAndTouch_withBlankToken_returnsEmptyWithoutQueryingRepository() {
        assertThat(service.validateAndTouch("")).isEmpty();
        assertThat(service.validateAndTouch(null)).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void list_returnsAllTokensOrderedByCreatedAtDesc() {
        McpAccessToken t1 = McpAccessToken.builder().id(1L).name("A").revoked(false).build();
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(t1));

        assertThat(service.list()).hasSize(1);
        assertThat(service.list().get(0).name()).isEqualTo("A");
    }

    private McpTokenGeneratedResponse generateWithCapture() {
        ArgumentCaptor<McpAccessToken> captor = ArgumentCaptor.forClass(McpAccessToken.class);
        when(repository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        McpTokenGeneratedResponse response = service.generate(new McpTokenCreateRequest("t", null));
        reset(repository);
        return response;
    }

    private String hashOf(String raw) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.Base64.getEncoder().encodeToString(digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
