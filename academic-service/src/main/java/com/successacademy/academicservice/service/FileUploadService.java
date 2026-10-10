package com.successacademy.academicservice.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".pdf", ".docx", ".doc", ".txt", ".jpg", ".jpeg", ".png", ".webp"
    );
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "text/plain",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Path rootStorageDir = Paths.get("uploads", "academic", "submissions").toAbsolutePath().normalize();

    public FileUploadService() {
        try {
            Files.createDirectories(rootStorageDir);
        } catch (IOException e) {
            log.error("Failed to initialize file storage directory: {}", e.getMessage());
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentUploadResult {
        private String fileUrl;
        private String fileName;
        private Long fileSize;
        private String fileType;
    }

    public AttachmentUploadResult uploadSubmissionFile(Long activityId, Long studentId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required and cannot be empty.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds maximum allowed size of 10MB.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name.");
        }

        // Prevent path traversal
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Path traversal sequence detected in file name.");
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File type not permitted. Allowed extensions: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String cleanContentType = contentType.toLowerCase().split(";")[0].trim();
            if (!ALLOWED_CONTENT_TYPES.contains(cleanContentType) && !cleanContentType.equals("application/octet-stream")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content-Type not permitted: " + cleanContentType);
            }
        }

        try {
            String safeStorageName = "sub_act" + activityId + "_st" + studentId + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
            Path destination = rootStorageDir.resolve(safeStorageName).normalize();

            // Verify final destination remains inside rootStorageDir
            if (!destination.startsWith(rootStorageDir)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target storage path.");
            }

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/academic-service/academic/activities/" + activityId + "/submissions/attachment/" + safeStorageName;

            return AttachmentUploadResult.builder()
                    .fileUrl(fileUrl)
                    .fileName(originalFilename)
                    .fileSize(file.getSize())
                    .fileType(contentType != null ? contentType : "application/octet-stream")
                    .build();
        } catch (IOException e) {
            log.error("Failed to store submission attachment: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store uploaded file.");
        }
    }

    public Resource loadFileAsResource(String filename) {
        try {
            if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name parameter.");
            }
            Path filePath = rootStorageDir.resolve(filename).normalize();
            if (!filePath.startsWith(rootStorageDir)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Access outside storage root denied.");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Requested file not found.");
            }
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File URL malformed.");
        }
    }
}
