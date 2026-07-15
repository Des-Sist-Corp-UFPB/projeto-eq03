package com.cristiane.salon.models.ai.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.ai.dto.AiConfigRequest;
import com.cristiane.salon.models.ai.dto.AiConfigResponse;
import com.cristiane.salon.models.ai.entity.AiConfig;
import com.cristiane.salon.models.ai.repository.AiConfigRepository;
import com.cristiane.salon.security.crypto.AiEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiConfigServiceTest {

    @Mock
    private AiConfigRepository repository;

    private AiEncryptionUtil encryptionUtil;

    private AiConfigService service;

    @BeforeEach
    void setUp() {
        String key32Bytes = Base64.getEncoder().encodeToString("unit-test-key-with-32-bytes-len!".getBytes());
        encryptionUtil = new AiEncryptionUtil(key32Bytes);
        service = new AiConfigService(repository, encryptionUtil);
    }

    private AiConfig existingConfig() {
        return AiConfig.builder()
                .id(AiConfig.SINGLETON_ID)
                .baseUrl("https://llm.rodrigor.com")
                .model("gpt-4o-mini")
                .apiKeyEncrypted(null)
                .temperature(new BigDecimal("0.30"))
                .maxTokens(500)
                .enabled(false)
                .dailyCallBudget(200)
                .build();
    }

    @Test
    void get_whenNoApiKeySet_returnsResponseWithoutMaskedKey() {
        when(repository.findById(AiConfig.SINGLETON_ID)).thenReturn(Optional.of(existingConfig()));

        AiConfigResponse response = service.get();

        assertThat(response.apiKeyConfigured()).isFalse();
        assertThat(response.apiKeyMasked()).isNull();
        assertThat(response.baseUrl()).isEqualTo("https://llm.rodrigor.com");
    }

    @Test
    void get_whenApiKeySet_returnsMaskedKeyNeverPlainText() {
        AiConfig config = existingConfig();
        config.setApiKeyEncrypted(encryptionUtil.encrypt("sk-ivdkbnUwyFCPx5Cinwj_UBVz4ijT_S_YEqgZvWesymE"));
        when(repository.findById(AiConfig.SINGLETON_ID)).thenReturn(Optional.of(config));

        AiConfigResponse response = service.get();

        assertThat(response.apiKeyConfigured()).isTrue();
        assertThat(response.apiKeyMasked()).isEqualTo("sk-•••••WesymE");
        assertThat(response.apiKeyMasked()).doesNotContain("ivdkbnUwyFCPx5Cinwj");
    }

    @Test
    void get_whenConfigMissing_throwsResourceNotFound() {
        when(repository.findById(AiConfig.SINGLETON_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get()).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_withNewApiKey_encryptsBeforePersisting() {
        AiConfig config = existingConfig();
        when(repository.findById(AiConfig.SINGLETON_ID)).thenReturn(Optional.of(config));
        when(repository.save(any(AiConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        AiConfigRequest request = new AiConfigRequest(
                "https://llm.rodrigor.com", "gpt-4o", "sk-new-secret-key",
                new BigDecimal("0.5"), 800, true, 300
        );

        AiConfigResponse response = service.update(request);

        assertThat(config.getApiKeyEncrypted()).isNotEqualTo("sk-new-secret-key");
        assertThat(encryptionUtil.decrypt(config.getApiKeyEncrypted())).isEqualTo("sk-new-secret-key");
        assertThat(response.model()).isEqualTo("gpt-4o");
        assertThat(response.enabled()).isTrue();
    }

    @Test
    void update_withBlankApiKey_keepsPreviousEncryptedKey() {
        AiConfig config = existingConfig();
        String previousEncrypted = encryptionUtil.encrypt("sk-previous-key");
        config.setApiKeyEncrypted(previousEncrypted);
        when(repository.findById(AiConfig.SINGLETON_ID)).thenReturn(Optional.of(config));
        when(repository.save(any(AiConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        AiConfigRequest request = new AiConfigRequest(
                "https://llm.rodrigor.com", "gpt-4o-mini", "",
                new BigDecimal("0.3"), 500, true, 200
        );

        service.update(request);

        assertThat(config.getApiKeyEncrypted()).isEqualTo(previousEncrypted);
    }

    @Test
    void update_withUnsupportedModel_throwsBadRequest() {
        AiConfigRequest request = new AiConfigRequest(
                "https://llm.rodrigor.com", "modelo-inexistente", null,
                new BigDecimal("0.3"), 500, true, 200
        );

        assertThatThrownBy(() -> service.update(request)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void update_withNonHttpsBaseUrl_throwsBadRequest() {
        AiConfigRequest request = new AiConfigRequest(
                "http://llm.rodrigor.com", "gpt-4o-mini", null,
                new BigDecimal("0.3"), 500, true, 200
        );

        assertThatThrownBy(() -> service.update(request)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repository);
    }
}
