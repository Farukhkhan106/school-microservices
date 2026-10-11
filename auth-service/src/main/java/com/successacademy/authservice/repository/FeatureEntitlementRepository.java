package com.successacademy.authservice.repository;

import com.successacademy.authservice.model.entity.FeatureEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureEntitlementRepository extends JpaRepository<FeatureEntitlement, Long> {

    List<FeatureEntitlement> findByTenantKey(String tenantKey);

    List<FeatureEntitlement> findByTenantKeyAndEnvironmentKey(String tenantKey, String environmentKey);

    List<FeatureEntitlement> findByTenantKeyAndEnvironmentKeyIsNull(String tenantKey);

    Optional<FeatureEntitlement> findByTenantKeyAndEnvironmentKeyAndFeatureKey(
            String tenantKey, String environmentKey, String featureKey);

    Optional<FeatureEntitlement> findByTenantKeyAndEnvironmentKeyIsNullAndFeatureKey(
            String tenantKey, String featureKey);
}
