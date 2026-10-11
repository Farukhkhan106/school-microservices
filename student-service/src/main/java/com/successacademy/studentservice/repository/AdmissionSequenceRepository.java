package com.successacademy.studentservice.repository;

import com.successacademy.studentservice.model.AdmissionSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdmissionSequenceRepository extends JpaRepository<AdmissionSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s FROM AdmissionSequence s
        WHERE s.tenantId = :tenantId
          AND s.sequenceType = :seqType
          AND s.sequenceYear = :seqYear
    """)
    Optional<AdmissionSequence> findForUpdate(
            @Param("tenantId") String tenantId,
            @Param("seqType") String seqType,
            @Param("seqYear") Integer seqYear
    );
}
