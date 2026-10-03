package com.successacademy.academicservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkEntryItem {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    private String studentName;
    private String rollNo;
    private BigDecimal marksObtained; // Nullable if absent
    private boolean isAbsent;
    private String remarks;
}
