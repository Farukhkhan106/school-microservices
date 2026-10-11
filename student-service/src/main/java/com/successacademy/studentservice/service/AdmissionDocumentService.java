package com.successacademy.studentservice.service;

import com.successacademy.studentservice.dto.AdmissionDocumentResponse;
import com.successacademy.studentservice.dto.DocumentVerificationRequest;
import com.successacademy.studentservice.model.*;
import com.successacademy.studentservice.repository.AdmissionApplicationRepository;
import com.successacademy.studentservice.repository.AdmissionDocumentRepository;
import com.successacademy.studentservice.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdmissionDocumentService {

    private final AdmissionDocumentRepository documentRepository;
    private final AdmissionApplicationRepository applicationRepository;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "jpg", "jpeg", "png", "webp"
    );

    private final Path rootStorageDir = Paths.get("uploads", "admissions");

    @Transactional
    public AdmissionDocumentResponse uploadDocument(Long applicationId,
                                                    DocumentType documentType,
                                                    MultipartFile file,
                                                    String uploadedBy) {
        String tenantId = TenantContext.getTenantId();
        AdmissionApplication app = applicationRepository.findByIdAndTenantId(applicationId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission application not found"));

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required and cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds maximum limit of 10MB");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        // Path traversal defense
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name: Potential path traversal detected");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported file extension. Allowed: " + ALLOWED_EXTENSIONS);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Unsupported MIME type. Allowed: " + ALLOWED_MIME_TYPES);
        }

        try {
            Path tenantDir = rootStorageDir.resolve(tenantId).normalize();
            if (!Files.exists(tenantDir)) {
                Files.createDirectories(tenantDir);
            }

            String storedFileName = UUID.randomUUID() + "." + extension;
            Path targetLocation = tenantDir.resolve(storedFileName).normalize();

            // Ensure destination is within tenant directory
            if (!targetLocation.startsWith(tenantDir)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Security violation: Destination path escaped storage root");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            AdmissionDocument document = AdmissionDocument.builder()
                    .applicationId(app.getId())
                    .tenantId(tenantId)
                    .documentType(documentType != null ? documentType : DocumentType.OTHER)
                    .originalFileName(originalFilename)
                    .storageReference(storedFileName)
                    .mimeType(contentType)
                    .fileSizeBytes(file.getSize())
                    .status(DocumentStatus.UPLOADED)
                    .uploadedBy(uploadedBy != null ? uploadedBy : "system")
                    .uploadedAt(LocalDateTime.now())
                    .build();

            AdmissionDocument saved = documentRepository.save(document);
            log.info("Document uploaded successfully: id={} type={} app={} tenant={}",
                    saved.getId(), saved.getDocumentType(), applicationId, tenantId);

            return mapToResponse(saved);
        } catch (IOException e) {
            log.error("Failed to store file for application {}: {}", applicationId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file on server");
        }
    }

    @Transactional(readOnly = true)
    public List<AdmissionDocumentResponse> getDocumentsForApplication(Long applicationId) {
        String tenantId = TenantContext.getTenantId();
        return documentRepository.findByApplicationIdAndTenantId(applicationId, tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Resource loadDocumentAsResource(Long documentId) {
        String tenantId = TenantContext.getTenantId();
        AdmissionDocument doc = documentRepository.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        try {
            Path filePath = rootStorageDir.resolve(tenantId).resolve(doc.getStorageReference()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found on disk");
            }
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid file path URL");
        }
    }

    @Transactional(readOnly = true)
    public AdmissionDocument getDocument(Long documentId) {
        String tenantId = TenantContext.getTenantId();
        return documentRepository.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    @Transactional
    public AdmissionDocumentResponse verifyDocument(Long documentId,
                                                    DocumentVerificationRequest request,
                                                    String verifiedBy) {
        String tenantId = TenantContext.getTenantId();
        AdmissionDocument doc = documentRepository.findByIdAndTenantId(documentId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (request.getStatus() == DocumentStatus.REJECTED) {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejection reason is mandatory when rejecting a document");
            }
            doc.setStatus(DocumentStatus.REJECTED);
            doc.setRejectionReason(request.getRejectionReason().trim());
        } else if (request.getStatus() == DocumentStatus.VERIFIED) {
            doc.setStatus(DocumentStatus.VERIFIED);
            doc.setRejectionReason(null);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification status must be VERIFIED or REJECTED");
        }

        doc.setVerifiedBy(verifiedBy != null ? verifiedBy : "staff");
        doc.setVerifiedAt(LocalDateTime.now());

        AdmissionDocument saved = documentRepository.save(doc);
        log.info("Document id={} verification updated to {} by {} for tenant={}",
                saved.getId(), saved.getStatus(), verifiedBy, tenantId);

        // Check if all documents for the application are verified
        AdmissionApplication app = applicationRepository.findByIdAndTenantId(doc.getApplicationId(), tenantId).orElse(null);
        if (app != null) {
            List<AdmissionDocument> allDocs = documentRepository.findByApplicationIdAndTenantId(app.getId(), tenantId);
            boolean allVerified = !allDocs.isEmpty() && allDocs.stream().allMatch(d -> d.getStatus() == DocumentStatus.VERIFIED);
            if (allVerified && (app.getStatus() == AdmissionStatus.UNDER_REVIEW || app.getStatus() == AdmissionStatus.DOCUMENTS_REQUIRED)) {
                app.setStatus(AdmissionStatus.DOCUMENT_VERIFIED);
                applicationRepository.save(app);
                log.info("All documents verified for application {}. Status changed to DOCUMENT_VERIFIED", app.getId());
            }
        }

        return mapToResponse(saved);
    }

    public AdmissionDocumentResponse mapToResponse(AdmissionDocument doc) {
        return AdmissionDocumentResponse.builder()
                .id(doc.getId())
                .applicationId(doc.getApplicationId())
                .tenantId(doc.getTenantId())
                .documentType(doc.getDocumentType())
                .originalFileName(doc.getOriginalFileName())
                .storageReference(doc.getStorageReference())
                .mimeType(doc.getMimeType())
                .fileSize(doc.getFileSizeBytes())
                .status(doc.getStatus())
                .uploadedBy(doc.getUploadedBy())
                .uploadedAt(doc.getUploadedAt())
                .verifiedBy(doc.getVerifiedBy())
                .verifiedAt(doc.getVerifiedAt())
                .rejectionReason(doc.getRejectionReason())
                .build();
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }
}
