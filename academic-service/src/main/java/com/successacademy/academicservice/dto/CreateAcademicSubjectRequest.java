package com.successacademy.academicservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAcademicSubjectRequest {
    @NotBlank(message = "Subject code is required")
    private String subjectCode;

    @NotBlank(message = "Display name is required")
    private String displayName;

    private String subjectType;
    private Integer displayOrder;
    private String status;
}
