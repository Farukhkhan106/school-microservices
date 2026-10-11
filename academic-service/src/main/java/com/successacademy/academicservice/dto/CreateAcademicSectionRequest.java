package com.successacademy.academicservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAcademicSectionRequest {
    @NotBlank(message = "Section code is required")
    private String sectionCode;

    @NotBlank(message = "Display name is required")
    private String displayName;

    private Integer displayOrder;
    private String status;
}
