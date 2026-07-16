package com.cristiane.salon.models.ai.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.ai.client.OpenAiCompatibleChatClient;
import com.cristiane.salon.models.ai.dto.AiConfigRequest;
import com.cristiane.salon.models.ai.dto.AiConfigResponse;
import com.cristiane.salon.models.ai.dto.AiConfigTestRequest;
import com.cristiane.salon.models.ai.dto.AiConfigTestResponse;
import com.cristiane.salon.models.ai.entity.AiConfig;
import com.cristiane.salon.models.ai.entity.AiModel;
import com.cristiane.salon.models.ai.repository.AiConfigRepository;
import com.cristiane.salon.security.crypto.AiEncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiConfigService {

    private static final String TEST_SYSTEM_PROMPT =
            "Você é um verificador de conectividade. Responda apenas com o JSON {\"pong\": true}.";
    private static final String TEST_USER_PROMPT = "ping";

    private final AiConfigRepository repository;
    private final AiEncryptionUtil encryptionUtil;
    private final OpenAiCompatibleChatClient chatClient;

    public AiConfigResponse get() {
        AiConfig config = findSingleton();
        String decrypted = config.getApiKeyEncrypted() != null
                ? encryptionUtil.decrypt(config.getApiKeyEncrypted())
                : null;
        return AiConfigResponse.fromEntity(config, decrypted);
    }

    /** Usado internamente pelo motor de recomendações — nunca exposto via controller. */
    public AiConfig getDecryptedForInternalUse() {
        return findSingleton();
    }

    public String getDecryptedApiKey(AiConfig config) {
        return config.getApiKeyEncrypted() != null ? encryptionUtil.decrypt(config.getApiKeyEncrypted()) : null;
    }

    @Transactional
    public AiConfigResponse update(AiConfigRequest request) {
        AiModel.fromWireName(request.model()); // lança BadRequest-like se o modelo não for um dos liberados
        if (request.baseUrl() != null && !request.baseUrl().startsWith("https://")) {
            throw new BadRequestException("A URL base deve usar HTTPS");
        }

        AiConfig config = findSingleton();
        config.setBaseUrl(request.baseUrl());
        config.setModel(request.model());
        config.setTemperature(request.temperature());
        config.setMaxTokens(request.maxTokens());
        config.setEnabled(request.enabled());
        config.setDailyCallBudget(request.dailyCallBudget());

        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            config.setApiKeyEncrypted(encryptionUtil.encrypt(request.apiKey()));
        }

        config.setUpdatedBy(currentUserEmail());
        config.setUpdatedAt(LocalDateTime.now());

        AiConfig saved = repository.save(config);
        String decrypted = saved.getApiKeyEncrypted() != null ? encryptionUtil.decrypt(saved.getApiKeyEncrypted()) : null;
        return AiConfigResponse.fromEntity(saved, decrypted);
    }

    /**
     * Diagnóstico ad-hoc de conectividade — usa os valores atuais do formulário (mesmo antes de
     * salvar), nunca grava em {@code tb_ai_call_log} nem conta no orçamento diário, e funciona
     * independente do estado de {@code enabled}/feature flag.
     */
    public AiConfigTestResponse testConnection(AiConfigTestRequest request) {
        if (!request.baseUrl().startsWith("https://")) {
            return new AiConfigTestResponse(false, "A URL base deve usar HTTPS", null);
        }
        try {
            AiModel.fromWireName(request.model());
        } catch (RuntimeException ex) {
            return new AiConfigTestResponse(false, "Modelo não suportado: " + request.model(), null);
        }

        String apiKey = request.apiKey() != null && !request.apiKey().isBlank()
                ? request.apiKey()
                : resolveSavedApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return new AiConfigTestResponse(false, "Nenhuma chave de API informada nem salva", null);
        }

        long start = System.currentTimeMillis();
        try {
            chatClient.complete(
                    request.baseUrl(), apiKey, request.model(),
                    BigDecimal.ZERO, 16, TEST_SYSTEM_PROMPT, TEST_USER_PROMPT
            );
            long latency = System.currentTimeMillis() - start;
            return new AiConfigTestResponse(true, "Conexão estabelecida com sucesso", latency);
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;
            log.warn("Teste de conexão de IA falhou: {}", ex.getMessage());
            return new AiConfigTestResponse(false, "Falha ao conectar: " + ex.getMessage(), latency);
        }
    }

    private String resolveSavedApiKey() {
        AiConfig config = findSingleton();
        return config.getApiKeyEncrypted() != null ? encryptionUtil.decrypt(config.getApiKeyEncrypted()) : null;
    }

    private AiConfig findSingleton() {
        return repository.findById(AiConfig.SINGLETON_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Configuração de IA não encontrada"));
    }

    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
