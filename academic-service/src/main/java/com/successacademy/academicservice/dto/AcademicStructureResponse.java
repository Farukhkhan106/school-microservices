package com.successacademy.academicservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicStructureResponse {
    private String tenantId;
    private List<AcademicClassDto> classes;
    private List<AcademicSectionDto> sections;
    private List<AcademicSubjectDto> subjects;
}
