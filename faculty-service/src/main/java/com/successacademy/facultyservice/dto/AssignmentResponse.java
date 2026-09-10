package com.successacademy.facultyservice.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AssignmentResponse {
    private Long id;
    private Long teacherId;
    private String teacherName;    // denormalized for display
    private String studentClass;
    private String section;
    private String subject;
    private String status;

    /** "8-A" convenience label. */
    public String getClassSection() {
        return studentClass + "-" + section;
    }
}