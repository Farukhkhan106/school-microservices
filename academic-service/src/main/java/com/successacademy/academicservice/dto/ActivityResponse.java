package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.ActivityStatus;
import com.successacademy.academicservice.model.ActivityType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityResponse {

    private Long id;
    private String title;
    private String description;
    private ActivityType activityType;
    private String studentClass;
    private String section;
    private String subject;
    private Long teacherId;
    private String teacherName;
    private LocalDate assignedDate;
    private LocalDate dueDate;
    private BigDecimal maxMarks;
    private BigDecimal passingMarks;
    private ActivityStatus status;
    private String instructions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
