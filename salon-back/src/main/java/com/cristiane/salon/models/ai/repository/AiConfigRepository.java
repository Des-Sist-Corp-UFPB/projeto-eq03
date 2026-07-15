package com.cristiane.salon.models.ai.repository;

import com.cristiane.salon.models.ai.entity.AiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiConfigRepository extends JpaRepository<AiConfig, Long> {
}
