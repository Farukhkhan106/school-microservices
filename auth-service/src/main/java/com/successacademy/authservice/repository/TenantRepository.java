package com.successacademy.authservice.repository;

import com.successacademy.authservice.model.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByTenantKey(String tenantKey);
    boolean existsByTenantKey(String tenantKey);
}
