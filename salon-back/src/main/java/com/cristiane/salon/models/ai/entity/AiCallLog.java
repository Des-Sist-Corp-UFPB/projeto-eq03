package com.cristiane.salon.models.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de toda chamada ao provedor de IA — usado para orçamento diário/rate limit
 * e para auditoria de custo, independente de o chamador ser a UI (REST) ou um cliente MCP.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_ai_call_log")
public class AiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** "USER" (chamada via UI/REST) ou "MCP_TOKEN" (chamada via servidor MCP). */
    @Column(name = "caller_type", nullable = false, length = 20)
    private String callerType;

    @Column(name = "caller_id", length = 100)
    private String callerId;

    /** Ex.: {@code FINANCEIRO}, {@code RETENCAO} — mesmo valor de {@link RecommendationType}. */
    @Column(name = "call_type", nullable = false, length = 50)
    private String callType;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
