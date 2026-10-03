package com.successacademy.facultyservice.service;

import com.successacademy.facultyservice.dto.FacultyDocumentResponse;
import com.successacademy.facultyservice.model.Faculty;
import com.successacademy.facultyservice.model.FacultyDocument;
import com.successacademy.facultyservice.repository.FacultyDocumentRepository;
import com.successacademy.facultyservice.repository.FacultyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacultyDocumentServiceImpl implements FacultyDocumentService {

    private final FacultyDocumentRepository documentRepository;
    private final FacultyRepository facultyRepository;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    @Override
    @Transactional
    public FacultyDocumentResponse uploadDocument(Long facultyId, String documentType, String documentName,
                                                  String documentNumber, MultipartFile file, Long actorUserId) {
        if (facultyId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faculty ID is required.");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File cannot be empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds the 10 MB limit.");
        }

        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found with id: " + facultyId));

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String storedName = "fac_doc_" + facultyId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        try {
            Path uploadDir = Paths.get("uploads", "faculty", "docs").toAbsolutePath().normalize();
            File dir = uploadDir.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            Path targetLocation = uploadDir.resolve(storedName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/faculty-service/uploads/faculty/docs/" + storedName;

            FacultyDocument doc = FacultyDocument.builder()
                    .facultyId(facultyId)
                    .documentType(documentType != null ? documentType.toUpperCase() : "OTHER")
                    .documentName(documentName != null ? documentName : (originalFilename != null ? originalFilename : "Document"))
                    .documentNumber(documentNumber)
                    .fileName(originalFilename != null ? originalFilename : storedName)
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .uploadedBy(actorUserId)
                    .build();

            FacultyDocument saved = documentRepository.save(doc);
            return mapToResponse(saved);
        } catch (IOException e) {
            log.error("Failed to store faculty document", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file: " + e.getMessage());
        }
    }

    @Override
    public List<FacultyDocumentResponse> getDocumentsForFaculty(Long facultyId) {
        if (!facultyRepository.existsById(facultyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found with id: " + facultyId);
        }
        return documentRepository.findByFacultyIdOrderByCreatedAtDesc(facultyId).stream()
                .map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId, Long actorUserId) {
        FacultyDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found with id: " + documentId));

        // Delete physical file if exists
        try {
            String url = doc.getFileUrl();
            if (url != null && url.contains("/faculty/docs/")) {
                String filename = url.substring(url.lastIndexOf("/") + 1);
                Path path = Paths.get("uploads", "faculty", "docs", filename).toAbsolutePath().normalize();
                Files.deleteIfExists(path);
            }
        } catch (Exception e) {
            log.warn("Could not delete physical file: {}", e.getMessage());
        }

        documentRepository.delete(doc);
    }

    private FacultyDocumentResponse mapToResponse(FacultyDocument d) {
        return FacultyDocumentResponse.builder()
                .id(d.getId())
                .facultyId(d.getFacultyId())
                .documentName(d.getDocumentName())
                .documentType(d.getDocumentType())
                .documentNumber(d.getDocumentNumber())
                .fileName(d.getFileName())
                .fileUrl(d.getFileUrl())
                .fileSize(d.getFileSize())
                .contentType(d.getContentType())
                .uploadedBy(d.getUploadedBy())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
