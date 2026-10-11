package com.successacademy.studentservice;

import com.successacademy.studentservice.dto.AdmissionDocumentResponse;
import com.successacademy.studentservice.dto.DocumentVerificationRequest;
import com.successacademy.studentservice.model.*;
import com.successacademy.studentservice.repository.AdmissionApplicationRepository;
import com.successacademy.studentservice.repository.AdmissionDocumentRepository;
import com.successacademy.studentservice.security.TenantContext;
import com.successacademy.studentservice.service.AdmissionDocumentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionDocumentServiceTest {

    @Mock
    private AdmissionDocumentRepository documentRepository;

    @Mock
    private AdmissionApplicationRepository applicationRepository;

    @InjectMocks
    private AdmissionDocumentService documentService;

    @BeforeEach
    void setup() {
        TenantContext.setTenantId("tenant-test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Upload valid PDF document succeeds")
    void testUploadValidPdf() {
        AdmissionApplication app = AdmissionApplication.builder()
                .id(401L)
                .tenantId("tenant-test")
                .build();

        when(applicationRepository.findByIdAndTenantId(401L, "tenant-test")).thenReturn(Optional.of(app));
        when(documentRepository.save(any(AdmissionDocument.class))).thenAnswer(inv -> {
            AdmissionDocument d = inv.getArgument(0);
            d.setId(901L);
            return d;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file", "birth_certificate.pdf", "application/pdf", "dummy pdf content".getBytes()
        );

        AdmissionDocumentResponse resp = documentService.uploadDocument(401L, DocumentType.BIRTH_CERTIFICATE, file, "parent_user");

        assertNotNull(resp);
        assertEquals(901L, resp.getId());
        assertEquals("birth_certificate.pdf", resp.getOriginalFileName());
        assertEquals(DocumentType.BIRTH_CERTIFICATE, resp.getDocumentType());
        assertEquals(DocumentStatus.UPLOADED, resp.getStatus());
    }

    @Test
    @DisplayName("Path Traversal Protection: Reject file names with .. or slashes")
    void testPathTraversalRejected() {
        AdmissionApplication app = AdmissionApplication.builder().id(402L).tenantId("tenant-test").build();
        when(applicationRepository.findByIdAndTenantId(402L, "tenant-test")).thenReturn(Optional.of(app));

        MockMultipartFile malicious = new MockMultipartFile(
                "file", "../../../etc/passwd.pdf", "application/pdf", "malicious payload".getBytes()
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                documentService.uploadDocument(402L, DocumentType.OTHER, malicious, "attacker"));

        assertTrue(ex.getReason().contains("traversal"));
    }

    @Test
    @DisplayName("Security: Reject executable and unsupported media types")
    void testUnsupportedExtensionRejected() {
        AdmissionApplication app = AdmissionApplication.builder().id(403L).tenantId("tenant-test").build();
        when(applicationRepository.findByIdAndTenantId(403L, "tenant-test")).thenReturn(Optional.of(app));

        MockMultipartFile executable = new MockMultipartFile(
                "file", "exploit.exe", "application/x-msdownload", "binary content".getBytes()
        );

        assertThrows(ResponseStatusException.class, () ->
                documentService.uploadDocument(403L, DocumentType.OTHER, executable, "attacker"));
    }

    @Test
    @DisplayName("Verification: Reject document requires mandatory rejection reason")
    void testRejectDocumentRequiresReason() {
        AdmissionDocument doc = AdmissionDocument.builder()
                .id(905L)
                .tenantId("tenant-test")
                .status(DocumentStatus.UPLOADED)
                .build();

        when(documentRepository.findByIdAndTenantId(905L, "tenant-test")).thenReturn(Optional.of(doc));

        DocumentVerificationRequest req = DocumentVerificationRequest.builder()
                .status(DocumentStatus.REJECTED)
                .rejectionReason("")
                .build();

        assertThrows(ResponseStatusException.class, () ->
                documentService.verifyDocument(905L, req, "staff"));
    }
}
