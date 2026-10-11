package com.successacademy.authservice.repository;

import com.successacademy.authservice.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndTenantId(String username, String tenantId);

    Optional<User> findByIdAndTenantId(Long id, String tenantId);

    Optional<User> findByTeacherId(Long teacherId);

    Optional<User> findByStudentId(Long studentId);

    Optional<User> findByStaffId(Long staffId);

    boolean existsByUsernameAndTenantId(String username, String tenantId);

    boolean existsByEmailAndTenantId(String email, String tenantId);

    Page<User> findByTenantId(String tenantId, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE u.tenantId = :tenantId
          AND (:status IS NULL OR u.status = :status)
          AND (:userType IS NULL OR u.userType = :userType)
          AND (:role IS NULL OR u.role = :role)
          AND (
            :searchTerm IS NULL
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
            OR u.phone LIKE CONCAT('%', :searchTerm, '%')
          )
    """)
    Page<User> searchUsers(
            @Param("tenantId") String tenantId,
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            @Param("userType") String userType,
            @Param("role") String role,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(ur) FROM UserRole ur
        WHERE ur.tenantId = :tenantId
          AND ur.role.roleCode = :roleCode
          AND ur.user.status = 'ACTIVE'
          AND ur.user.id <> :excludeUserId
    """)
    long countOtherActiveUsersWithRole(
            @Param("tenantId") String tenantId,
            @Param("roleCode") String roleCode,
            @Param("excludeUserId") Long excludeUserId
    );
}
