package com.successacademy.authservice.repository;

import com.successacademy.authservice.model.entity.TenantConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantConfigurationRepository extends JpaRepository<TenantConfiguration, Long> {

    List<TenantConfiguration> findByVersionId(Long versionId);

    Optional<TenantConfiguration> findByVersionIdAndCategory(Long versionId, String category);

    List<TenantConfiguration> findByTenantKeyAndEnvironmentKey(String tenantKey, String environmentKey);

    List<TenantConfiguration> findByTenantKeyAndEnvironmentKeyIsNull(String tenantKey);
}
