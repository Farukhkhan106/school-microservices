package com.successacademy.studentservice.repository;

import com.successacademy.studentservice.model.Guardian;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    Optional<Guardian> findByIdAndTenantId(Long id, String tenantId);

    Optional<Guardian> findByTenantIdAndPhone(String tenantId, String phone);

    Optional<Guardian> findByTenantIdAndUserId(String tenantId, Long userId);

    List<Guardian> findByTenantId(String tenantId);

    Page<Guardian> findByTenantId(String tenantId, Pageable pageable);

    @Query("""
        SELECT g FROM Guardian g
        WHERE g.tenantId = :tenantId
          AND (:search IS NULL OR LOWER(g.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(g.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(g.phone) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(g.email) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<Guardian> searchGuardians(@Param("tenantId") String tenantId, @Param("search") String search, Pageable pageable);
}
