package com.successacademy.facultyservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyDocumentResponse {

    private Long id;
    private Long facultyId;
    private String documentName;
    private String documentType;
    private String documentNumber;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String contentType;
    private Long uploadedBy;
    private LocalDateTime createdAt;
}
