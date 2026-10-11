package com.successacademy.studentservice.dto;

import com.successacademy.studentservice.model.DocumentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVerificationRequest {
    @NotNull(message = "Document status is required")
    private DocumentStatus status; // VERIFIED or REJECTED

    private String rejectionReason;
}
