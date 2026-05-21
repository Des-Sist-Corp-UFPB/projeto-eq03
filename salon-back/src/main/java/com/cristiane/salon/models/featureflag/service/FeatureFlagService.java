package com.cristiane.salon.models.featureflag.service;

import com.cristiane.salon.exception.ResourceNotFoundException;
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
        flag.setEnabled(!flag.getEnabled());
        return repository.save(flag);
    }
}
