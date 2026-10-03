package com.successacademy.communicationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecipientDto {
    private Long userId;
    private String username;
    private String name;
    private String role; // ADMIN, TEACHER, STUDENT
    private String description; // e.g. "Class Teacher (10-A)", "Mathematics Teacher", "Class 10-A", "Principal"
    private Long referenceId; // studentId or teacherId
}
