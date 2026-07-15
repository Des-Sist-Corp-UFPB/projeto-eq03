package com.cristiane.salon.models.ai.dto;

import com.cristiane.salon.models.ai.entity.RecommendationType;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendationResult(
        RecommendationType type,
        List<RecommendationItem> items,
        LocalDateTime generatedAt,
        boolean fromCache
) {}
