package com.successacademy.academicservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionCreateRequest {

    @NotBlank(message = "Submission content cannot be empty")
    @Size(max = 5000, message = "Submission text cannot exceed 5000 characters")
    private String submissionText;
}
