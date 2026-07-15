package com.cristiane.salon.models.ai.repository;

import com.cristiane.salon.models.ai.entity.AiRecommendation;
import com.cristiane.salon.models.ai.entity.RecommendationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, Long> {

    Optional<AiRecommendation> findFirstByTypeOrderByGeneratedAtDesc(RecommendationType type);
}
