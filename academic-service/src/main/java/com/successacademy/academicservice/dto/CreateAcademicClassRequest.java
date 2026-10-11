package com.successacademy.academicservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAcademicClassRequest {
    @NotBlank(message = "Class code is required")
    private String classCode;

    @NotBlank(message = "Display name is required")
    private String displayName;

    private Integer displayOrder;
    private String category;
    private String status;
}
