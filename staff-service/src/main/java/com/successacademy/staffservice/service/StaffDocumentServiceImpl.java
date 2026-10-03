package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.StaffDocumentResponse;
import com.successacademy.staffservice.model.Staff;
import com.successacademy.staffservice.model.StaffDocument;
import com.successacademy.staffservice.repository.StaffDocumentRepository;
import com.successacademy.staffservice.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffDocumentServiceImpl implements StaffDocumentService {

    private final StaffDocumentRepository documentRepository;
    private final StaffRepository staffRepository;
    private final AuditLogService auditLogService;

    @Override
    public StaffDocumentResponse uploadDocument(
            Long staffId,
            String documentType,
            String documentNumber,
            LocalDate issueDate,
            LocalDate expiryDate,
            MultipartFile file,
            Long actorUserId
    ) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + staffId));

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required.");
        }

        String savedPath = saveFile(file, "doc_" + staffId);

        StaffDocument doc = StaffDocument.builder()
                .staffId(staff.getId())
                .documentType(documentType != null ? documentType.trim().toUpperCase() : "OTHER")
                .documentNumber(documentNumber)
                .fileUrl(savedPath)
                .issueDate(issueDate)
                .expiryDate(expiryDate)
                .status("ACTIVE")
                .uploadedBy(actorUserId)
                .build();

        StaffDocument saved = documentRepository.save(doc);
        auditLogService.log(actorUserId, staffId, "DOCUMENT_UPLOADED", null, doc.getDocumentType(), "Uploaded document " + doc.getDocumentType());
        return mapToResponse(saved);
    }

    @Override
    public List<StaffDocumentResponse> getDocumentsForStaff(Long staffId) {
        return documentRepository.findByStaffIdOrderByCreatedAtDesc(staffId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void deleteDocument(Long id, Long actorUserId) {
        StaffDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found with id: " + id));

        documentRepository.deleteById(id);
        auditLogService.log(actorUserId, doc.getStaffId(), "DOCUMENT_DELETED", doc.getDocumentType(), null, "Deleted document");
    }

    private String saveFile(MultipartFile file, String prefix) {
        String originalName = file.getOriginalFilename();
        String ext = ".pdf";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }

        try {
            Path uploadDir = Paths.get("uploads", "staff", "docs").toAbsolutePath().normalize();
            File dir = uploadDir.toFile();
            if (!dir.exists()) dir.mkdirs();

            String filename = prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path targetLocation = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/staff-service/uploads/staff/docs/" + filename;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file: " + e.getMessage());
        }
    }

    private StaffDocumentResponse mapToResponse(StaffDocument d) {
        String expiryStatus = "NO_EXPIRY";
        Long daysUntilExpiry = null;

        if (d.getExpiryDate() != null) {
            LocalDate today = LocalDate.now();
            daysUntilExpiry = ChronoUnit.DAYS.between(today, d.getExpiryDate());

            if (daysUntilExpiry < 0) {
                expiryStatus = "EXPIRED";
            } else if (daysUntilExpiry <= 30) {
                expiryStatus = "EXPIRING_SOON";
            } else {
                expiryStatus = "VALID";
            }
        }

        return StaffDocumentResponse.builder()
                .id(d.getId())
                .staffId(d.getStaffId())
                .documentType(d.getDocumentType())
                .documentNumber(d.getDocumentNumber())
                .fileUrl(d.getFileUrl())
                .issueDate(d.getIssueDate())
                .expiryDate(d.getExpiryDate())
                .status(d.getStatus())
                .expiryStatus(expiryStatus)
                .daysUntilExpiry(daysUntilExpiry)
                .uploadedBy(d.getUploadedBy())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
