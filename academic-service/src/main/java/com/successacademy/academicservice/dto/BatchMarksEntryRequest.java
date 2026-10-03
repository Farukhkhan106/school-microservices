package com.successacademy.academicservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchMarksEntryRequest {
    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    /**
     * Action to perform:
     * - "SAVE_DRAFT": Saves current state without strict validation
     * - "SUBMIT": Validates all students have marks or absent flag, transitions status to SUBMITTED
     */
    @NotNull(message = "Action must be SAVE_DRAFT or SUBMIT")
    private String action; // "SAVE_DRAFT" | "SUBMIT"

    @NotEmpty(message = "Marks entry list cannot be empty")
    @Valid
    private List<MarkEntryItem> entries;
}
