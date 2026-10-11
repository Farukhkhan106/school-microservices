package com.successacademy.studentservice.service;

import com.successacademy.studentservice.model.AdmissionSequence;
import com.successacademy.studentservice.repository.AdmissionSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdmissionSequenceService {

    private final AdmissionSequenceRepository sequenceRepository;

    /**
     * Generates a tenant-scoped collision-safe application number: ADM-YYYY-000001
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextApplicationNumber(String tenantId) {
        int year = LocalDate.now().getYear();
        long seq = getNextSequenceValue(tenantId, "APPLICATION", year);
        String applicationNumber = String.format("ADM-%d-%06d", year, seq);
        log.info("Generated application number: {} for tenant: {}", applicationNumber, tenantId);
        return applicationNumber;
    }

    /**
     * Generates a tenant-scoped collision-safe admission number: SA-YYYY-0001
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextAdmissionNumber(String tenantId) {
        int year = LocalDate.now().getYear();
        long seq = getNextSequenceValue(tenantId, "ADMISSION", year);
        String admissionNumber = String.format("SA-%d-%04d", year, seq);
        log.info("Generated admission number: {} for tenant: {}", admissionNumber, tenantId);
        return admissionNumber;
    }

    private synchronized long getNextSequenceValue(String tenantId, String type, int year) {
        String safeTenant = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId.trim();
        Optional<AdmissionSequence> existing = sequenceRepository.findForUpdate(safeTenant, type, year);

        AdmissionSequence sequence;
        if (existing.isPresent()) {
            sequence = existing.get();
            sequence.setCurrentValue(sequence.getCurrentValue() + 1);
        } else {
            sequence = AdmissionSequence.builder()
                    .tenantId(safeTenant)
                    .sequenceType(type)
                    .sequenceYear(year)
                    .currentValue(1L)
                    .build();
        }

        sequenceRepository.saveAndFlush(sequence);
        return sequence.getCurrentValue();
    }
}
