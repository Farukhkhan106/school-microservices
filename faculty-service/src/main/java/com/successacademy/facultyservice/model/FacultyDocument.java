package com.successacademy.facultyservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "faculty_documents", indexes = {
        @Index(name = "idx_faculty_doc_faculty_id", columnList = "faculty_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "faculty_id", nullable = false)
    private Long facultyId;

    @Column(name = "document_name", length = 255)
    private String documentName;

    // AADHAAR, PAN, RESUME, CONTRACT, CERTIFICATE, OTHER
    @Column(name = "document_type", length = 64)
    private String documentType;

    @Column(name = "document_number", length = 128)
    private String documentNumber;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 128)
    private String contentType;

    private Long uploadedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
