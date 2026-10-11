package com.successacademy.studentservice.dto;

import com.successacademy.studentservice.model.DocumentStatus;
import com.successacademy.studentservice.model.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionDocumentResponse {
    private Long id;
    private Long applicationId;
    private String tenantId;
    private DocumentType documentType;
    private String originalFileName;
    private String storageReference;
    private String mimeType;
    private Long fileSize;
    private DocumentStatus status;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private String verifiedBy;
    private LocalDateTime verifiedAt;
    private String rejectionReason;
}
