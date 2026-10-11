package com.successacademy.academicservice.dto;

import lombok.*;

import java.time.LocalDateTime;

public class AcademicOfferingDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClassSectionOfferingDto {
        private Long id;
        private String tenantId;
        private Long sessionId;
        private String classCode;
        private String sectionCode;
        private Integer capacity;
        private String roomNumber;
        private String status;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClassSubjectOfferingDto {
        private Long id;
        private String tenantId;
        private Long sessionId;
        private String classCode;
        private String subjectCode;
        private boolean isMandatory;
        private Integer weeklyPeriods;
        private String status;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateClassSectionOfferingRequest {
        private String classCode;
        private String sectionCode;
        private Integer capacity;
        private String roomNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateClassSubjectOfferingRequest {
        private String classCode;
        private String subjectCode;
        private Boolean isMandatory;
        private Integer weeklyPeriods;
    }
}
