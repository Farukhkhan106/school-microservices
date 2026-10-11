package com.successacademy.studentservice;

import com.successacademy.studentservice.model.AdmissionSequence;
import com.successacademy.studentservice.repository.AdmissionSequenceRepository;
import com.successacademy.studentservice.service.AdmissionSequenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionSequenceServiceTest {

    @Mock
    private AdmissionSequenceRepository sequenceRepository;

    @InjectMocks
    private AdmissionSequenceService sequenceService;

    private final int currentYear = LocalDate.now().getYear();

    @Test
    @DisplayName("Generate first application number for new tenant")
    void testFirstApplicationNumber() {
        when(sequenceRepository.findForUpdate(eq("tenant-alpha"), eq("APPLICATION"), eq(currentYear)))
                .thenReturn(Optional.empty());

        when(sequenceRepository.saveAndFlush(any(AdmissionSequence.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String appNumber = sequenceService.nextApplicationNumber("tenant-alpha");

        assertNotNull(appNumber);
        assertEquals(String.format("ADM-%d-000001", currentYear), appNumber);
        verify(sequenceRepository).saveAndFlush(argThat(seq ->
                seq.getCurrentValue() == 1L && "tenant-alpha".equals(seq.getTenantId())));
    }

    @Test
    @DisplayName("Generate sequential application numbers with collision-safe increment")
    void testIncrementalApplicationNumber() {
        AdmissionSequence existing = AdmissionSequence.builder()
                .id(10L)
                .tenantId("tenant-alpha")
                .sequenceType("APPLICATION")
                .sequenceYear(currentYear)
                .currentValue(42L)
                .build();

        when(sequenceRepository.findForUpdate(eq("tenant-alpha"), eq("APPLICATION"), eq(currentYear)))
                .thenReturn(Optional.of(existing));

        when(sequenceRepository.saveAndFlush(any(AdmissionSequence.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String appNumber = sequenceService.nextApplicationNumber("tenant-alpha");

        assertEquals(String.format("ADM-%d-000043", currentYear), appNumber);
        assertEquals(43L, existing.getCurrentValue());
    }

    @Test
    @DisplayName("Generate student admission number: SA-YYYY-0001")
    void testNextAdmissionNumber() {
        when(sequenceRepository.findForUpdate(eq("default"), eq("ADMISSION"), eq(currentYear)))
                .thenReturn(Optional.empty());

        when(sequenceRepository.saveAndFlush(any(AdmissionSequence.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String admNumber = sequenceService.nextAdmissionNumber("default");

        assertNotNull(admNumber);
        assertEquals(String.format("SA-%d-0001", currentYear), admNumber);
    }
}
