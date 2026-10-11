package com.successacademy.studentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "admission_documents",
    indexes = {
        @Index(name = "idx_adm_doc_tenant_app", columnList = "tenant_id, application_id"),
        @Index(name = "idx_adm_doc_tenant_status", columnList = "tenant_id, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(length = 150)
    private String title;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "storage_reference", nullable = false, length = 500)
    private String storageReference;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    public Long getFileSize() {
        return this.fileSizeBytes;
    }

    public void setFileSize(Long size) {
        this.fileSizeBytes = size;
    }

    @PrePersist
    protected void onCreate() {
        if (this.tenantId == null || this.tenantId.isBlank()) this.tenantId = "default";
        if (this.status == null) this.status = DocumentStatus.UPLOADED;
        if (this.uploadedAt == null) this.uploadedAt = LocalDateTime.now();
    }
}
