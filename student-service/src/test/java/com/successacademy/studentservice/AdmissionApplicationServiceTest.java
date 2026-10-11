package com.successacademy.studentservice;

import com.successacademy.studentservice.dto.AdmissionApplicationRequest;
import com.successacademy.studentservice.dto.AdmissionApplicationResponse;
import com.successacademy.studentservice.model.AdmissionApplication;
import com.successacademy.studentservice.model.AdmissionStatus;
import com.successacademy.studentservice.repository.AdmissionApplicationRepository;
import com.successacademy.studentservice.repository.AdmissionDocumentRepository;
import com.successacademy.studentservice.security.TenantContext;
import com.successacademy.studentservice.service.AdmissionApplicationService;
import com.successacademy.studentservice.service.AdmissionDocumentService;
import com.successacademy.studentservice.service.AdmissionSequenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionApplicationServiceTest {

    @Mock
    private AdmissionApplicationRepository applicationRepository;

    @Mock
    private AdmissionDocumentRepository documentRepository;

    @Mock
    private AdmissionSequenceService sequenceService;

    @Mock
    private AdmissionDocumentService documentService;

    @InjectMocks
    private AdmissionApplicationService applicationService;

    @BeforeEach
    void setup() {
        TenantContext.setTenantId("tenant-test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Create Draft Application successfully")
    void testCreateDraftApplication() {
        when(sequenceService.nextApplicationNumber("tenant-test")).thenReturn("ADM-2026-000001");
        when(applicationRepository.save(any(AdmissionApplication.class))).thenAnswer(inv -> {
            AdmissionApplication a = inv.getArgument(0);
            a.setId(101L);
            return a;
        });

        AdmissionApplicationRequest request = AdmissionApplicationRequest.builder()
                .academicSessionId(1L)
                .applyingClass("10")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(2010, 5, 15))
                .gender("MALE")
                .fatherName("Robert Doe")
                .fatherPhone("9876543210")
                .fatherEmail("robert@example.com")
                .addressLine1("123 Main St")
                .city("City")
                .state("State")
                .pincode("452001")
                .build();

        AdmissionApplicationResponse resp = applicationService.createApplication(request, "staff_user", false);

        assertNotNull(resp);
        assertEquals(101L, resp.getId());
        assertEquals("ADM-2026-000001", resp.getApplicationNumber());
        assertEquals(AdmissionStatus.DRAFT, resp.getStatus());
        assertEquals("John", resp.getApplicantFirstName());
        assertEquals("Doe", resp.getApplicantLastName());
    }

    @Test
    @DisplayName("Maker-Checker: Creator cannot approve their own application")
    void testMakerCheckerApprovalBlocked() {
        AdmissionApplication app = AdmissionApplication.builder()
                .id(202L)
                .tenantId("tenant-test")
                .applicationNumber("ADM-2026-000002")
                .status(AdmissionStatus.UNDER_REVIEW)
                .createdBy("principal_smith")
                .build();

        when(applicationRepository.findByIdAndTenantId(202L, "tenant-test")).thenReturn(Optional.of(app));

        // Approver is same as creator
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                applicationService.approveApplication(202L, "principal_smith", "Looks good"));

        assertTrue(ex.getReason().contains("Maker-Checker violation"));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Checker approves application when in DOCUMENT_VERIFIED state")
    void testCheckerApprovalSuccess() {
        AdmissionApplication app = AdmissionApplication.builder()
                .id(203L)
                .tenantId("tenant-test")
                .applicationNumber("ADM-2026-000003")
                .status(AdmissionStatus.DOCUMENT_VERIFIED)
                .createdBy("admission_clerk")
                .firstName("Alice")
                .lastName("Wonder")
                .build();

        when(applicationRepository.findByIdAndTenantId(203L, "tenant-test")).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(AdmissionApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        AdmissionApplicationResponse approved = applicationService.approveApplication(203L, "principal_jones", "Approved for admission");

        assertNotNull(approved);
        assertEquals(AdmissionStatus.APPROVED, approved.getStatus());
        assertEquals("principal_jones", approved.getApprovedBy());
        assertNotNull(approved.getApprovedAt());
    }

    @Test
    @DisplayName("Reject application requires reason")
    void testRejectApplicationRequiresReason() {
        AdmissionApplication app = AdmissionApplication.builder()
                .id(204L)
                .tenantId("tenant-test")
                .status(AdmissionStatus.UNDER_REVIEW)
                .build();

        when(applicationRepository.findByIdAndTenantId(204L, "tenant-test")).thenReturn(Optional.of(app));

        assertThrows(ResponseStatusException.class, () ->
                applicationService.rejectApplication(204L, "admin_user", ""));
    }
}
