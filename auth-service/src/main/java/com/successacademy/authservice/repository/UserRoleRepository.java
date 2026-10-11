package com.successacademy.authservice.repository;

import com.successacademy.authservice.model.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserIdAndTenantId(Long userId, String tenantId);

    List<UserRole> findByUserId(Long userId);

    Optional<UserRole> findByUserIdAndRoleIdAndTenantId(Long userId, Long roleId, String tenantId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND (ur.tenantId = :tenantId OR ur.tenantId = 'default')")
    List<UserRole> findEffectiveRolesForUser(@Param("userId") Long userId, @Param("tenantId") String tenantId);

    void deleteByUserIdAndRoleIdAndTenantId(Long userId, Long roleId, String tenantId);
}
