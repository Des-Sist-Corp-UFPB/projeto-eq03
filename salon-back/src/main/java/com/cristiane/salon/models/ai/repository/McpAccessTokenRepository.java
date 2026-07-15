package com.cristiane.salon.models.ai.repository;

import com.cristiane.salon.models.ai.entity.McpAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface McpAccessTokenRepository extends JpaRepository<McpAccessToken, Long> {

    Optional<McpAccessToken> findByTokenHash(String tokenHash);

    List<McpAccessToken> findAllByOrderByCreatedAtDesc();
}
