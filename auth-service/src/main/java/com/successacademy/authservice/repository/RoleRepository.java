package com.successacademy.authservice.repository;

import com.successacademy.authservice.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleCodeAndTenantId(String roleCode, String tenantId);

    Optional<Role> findByRoleCodeAndTenantIdIsNull(String roleCode);

    @Query("SELECT r FROM Role r WHERE (r.tenantId IS NULL OR r.tenantId = :tenantId) AND r.status = 'ACTIVE'")
    List<Role> findAvailableRolesForTenant(@Param("tenantId") String tenantId);

    List<Role> findByTenantId(String tenantId);

    List<Role> findByTenantIdIsNull();

    boolean existsByRoleCodeAndTenantId(String roleCode, String tenantId);
}
