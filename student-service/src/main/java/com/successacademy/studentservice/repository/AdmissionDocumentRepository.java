package com.successacademy.studentservice.repository;

import com.successacademy.studentservice.model.AdmissionDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdmissionDocumentRepository extends JpaRepository<AdmissionDocument, Long> {

    List<AdmissionDocument> findByTenantIdAndApplicationId(String tenantId, Long applicationId);

    default List<AdmissionDocument> findByApplicationIdAndTenantId(Long applicationId, String tenantId) {
        return findByTenantIdAndApplicationId(tenantId, applicationId);
    }

    Optional<AdmissionDocument> findByIdAndTenantId(Long id, String tenantId);

    long countByTenantIdAndApplicationId(String tenantId, Long applicationId);
}
