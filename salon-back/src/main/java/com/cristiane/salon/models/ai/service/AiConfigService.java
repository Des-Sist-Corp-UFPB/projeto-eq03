package com.cristiane.salon.models.ai.service;

import com.cristiane.salon.exception.BadRequestException;
import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.ai.dto.AiConfigRequest;
import com.cristiane.salon.models.ai.dto.AiConfigResponse;
import com.cristiane.salon.models.ai.entity.AiConfig;
import com.cristiane.salon.models.ai.entity.AiModel;
import com.cristiane.salon.models.ai.repository.AiConfigRepository;
import com.cristiane.salon.security.crypto.AiEncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiConfigService {

    private final AiConfigRepository repository;
    private final AiEncryptionUtil encryptionUtil;

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

    private AiConfig findSingleton() {
        return repository.findById(AiConfig.SINGLETON_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Configuração de IA não encontrada"));
    }

    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
