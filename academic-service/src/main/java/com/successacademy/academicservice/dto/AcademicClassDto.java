package com.successacademy.academicservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicClassDto {
    private Long id;
    private String tenantId;
    private String classCode;
    private String displayName;
    private Integer displayOrder;
    private String category;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
