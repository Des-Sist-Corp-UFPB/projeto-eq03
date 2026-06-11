package com.cristiane.salon.models.featureflag.service;

import com.cristiane.salon.exception.ResourceNotFoundException;
import com.cristiane.salon.models.audit.AuditLogService;
import com.cristiane.salon.models.featureflag.entity.FeatureFlag;
import com.cristiane.salon.models.featureflag.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagRepository repository;
    private final AuditLogService auditLogService;

    public boolean isEnabled(String name) {
        return repository.findById(name)
                .map(FeatureFlag::getEnabled)
                .orElse(false);
    }

    public List<FeatureFlag> findAll() {
        return repository.findAll();
    }

    @Transactional
    public FeatureFlag toggle(String name) {
        FeatureFlag flag = repository.findById(name)
                .orElseThrow(() -> new ResourceNotFoundException("Feature Flag não encontrada: " + name));
        
        boolean from = flag.getEnabled();
        boolean to = !from;
        
        flag.setEnabled(to);
        FeatureFlag saved = repository.save(flag);

        // Audit Log manual
        try {
            String details = String.format("{\"flag\":\"%s\",\"from\":%b,\"to\":%b}", name, from, to);
            
            Long userId = null;
            String userEmail = "SYSTEM";
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                userEmail = auth.getName();
                if (auth.getPrincipal() instanceof com.cristiane.salon.models.user.entity.User user) {
                    userId = user.getId();
                }
            }
            
            auditLogService.logAction(
                    userId,
                    userEmail,
                    "FEATURE_FLAG_TOGGLED",
                    "FeatureFlag",
                    null,
                    details,
                    "SUCCESS"
            );
        } catch (Exception e) {
            // Log silenciosamente
        }

        return saved;
    }
}
