package com.successacademy.facultyservice.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AssignmentRequest {
    private Long teacherId;
    private String studentClass;   // "8"
    private String section;        // "A"
    private String subject;        // "Mathematics"
    private String status;         // Active | Inactive (default Active)
}