package com.successacademy.authservice.repository;

import com.successacademy.authservice.model.entity.ConfigurationVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigurationVersionRepository extends JpaRepository<ConfigurationVersion, Long> {

    List<ConfigurationVersion> findByTenantKeyOrderByVersionNumberDesc(String tenantKey);

    Optional<ConfigurationVersion> findByTenantKeyAndEnvironmentKeyAndVersionNumber(
            String tenantKey, String environmentKey, Integer versionNumber);

    Optional<ConfigurationVersion> findByTenantKeyAndEnvironmentKeyIsNullAndVersionNumber(
            String tenantKey, Integer versionNumber);

    // Active published version for tenant + environment
    @Query("SELECT cv FROM ConfigurationVersion cv WHERE cv.tenantKey = :tenantKey " +
           "AND cv.environmentKey = :environmentKey AND cv.status = 'PUBLISHED' " +
           "ORDER BY cv.versionNumber DESC")
    List<ConfigurationVersion> findPublishedForTenantAndEnv(
            @Param("tenantKey") String tenantKey, @Param("environmentKey") String environmentKey);

    // Active published version for tenant across environments (environmentKey is null)
    @Query("SELECT cv FROM ConfigurationVersion cv WHERE cv.tenantKey = :tenantKey " +
           "AND cv.environmentKey IS NULL AND cv.status = 'PUBLISHED' " +
           "ORDER BY cv.versionNumber DESC")
    List<ConfigurationVersion> findPublishedForTenantGlobal(@Param("tenantKey") String tenantKey);

    @Query("SELECT MAX(cv.versionNumber) FROM ConfigurationVersion cv WHERE cv.tenantKey = :tenantKey")
    Integer findMaxVersionNumberByTenantKey(@Param("tenantKey") String tenantKey);
}
