package com.successacademy.studentservice.repository;

import com.successacademy.studentservice.model.AdmissionApplication;
import com.successacademy.studentservice.model.AdmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdmissionApplicationRepository extends JpaRepository<AdmissionApplication, Long> {

    Optional<AdmissionApplication> findByIdAndTenantId(Long id, String tenantId);

    Optional<AdmissionApplication> findByTenantIdAndApplicationNumber(String tenantId, String applicationNumber);

    List<AdmissionApplication> findByTenantId(String tenantId);

    default List<AdmissionApplication> findAllByTenantId(String tenantId) {
        return findByTenantId(tenantId);
    }

    List<AdmissionApplication> findByTenantIdAndStatus(String tenantId, AdmissionStatus status);

    List<AdmissionApplication> findByTenantIdAndAcademicSessionId(String tenantId, Long academicSessionId);

    @Query("""
        SELECT a FROM AdmissionApplication a
        WHERE a.tenantId = :tenantId
          AND (:status IS NULL OR a.status = :status)
          AND (:applyingClass IS NULL OR a.applyingClass = :applyingClass)
          AND (:sessionId IS NULL OR a.academicSessionId = :sessionId)
          AND (:search IS NULL OR LOWER(a.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(a.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(a.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(a.phone) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<AdmissionApplication> searchApplications(
            @Param("tenantId") String tenantId,
            @Param("status") AdmissionStatus status,
            @Param("applyingClass") String applyingClass,
            @Param("sessionId") Long sessionId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT a.status, COUNT(a) FROM AdmissionApplication a WHERE a.tenantId = :tenantId GROUP BY a.status")
    List<Object[]> countByStatusGrouped(@Param("tenantId") String tenantId);
}
