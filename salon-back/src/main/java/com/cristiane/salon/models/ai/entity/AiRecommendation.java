package com.cristiane.salon.models.ai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Cache do último resultado gerado por {@link RecommendationType}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_ai_recommendation")
public class AiRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationType type;

    /** JSON serializado de {@code List<RecommendationItem>}. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}
