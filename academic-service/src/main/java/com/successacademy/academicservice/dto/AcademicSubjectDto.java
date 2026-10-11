package com.successacademy.academicservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicSubjectDto {
    private Long id;
    private String tenantId;
    private String subjectCode;
    private String displayName;
    private String subjectType;
    private Integer displayOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
