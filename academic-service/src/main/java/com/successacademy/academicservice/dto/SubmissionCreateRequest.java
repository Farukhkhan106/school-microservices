package com.successacademy.academicservice.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionCreateRequest {

    @Size(max = 5000, message = "Submission text cannot exceed 5000 characters")
    private String submissionText;

    private String attachmentUrl;

    private String attachmentName;

    private Long attachmentSize;

    private String attachmentType;
}
