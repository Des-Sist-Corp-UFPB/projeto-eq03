package com.cristiane.salon.models.featureflag.repository;

import com.cristiane.salon.models.featureflag.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {
}
