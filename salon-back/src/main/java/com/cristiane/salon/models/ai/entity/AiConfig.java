package com.cristiane.salon.models.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuração única (singleton, id fixo em 1) do provedor de IA usado pelo motor de recomendações
 * e pelo servidor MCP. A API key nunca é armazenada em texto puro — ver {@link com.cristiane.salon.security.crypto.AiEncryptionUtil}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_ai_config")
public class AiConfig {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "api_key_encrypted", columnDefinition = "TEXT")
    private String apiKeyEncrypted;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal temperature;

    @Column(name = "max_tokens", nullable = false)
    private Integer maxTokens;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "daily_call_budget", nullable = false)
    private Integer dailyCallBudget;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
