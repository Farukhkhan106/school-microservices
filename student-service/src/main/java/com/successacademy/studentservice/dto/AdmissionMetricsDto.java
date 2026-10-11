package com.successacademy.studentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionMetricsDto {
    private long totalApplications;
    private long draftCount;
    private long submittedCount;
    private long underReviewCount;
    private long documentsRequiredCount;
    private long documentVerifiedCount;
    private long approvedCount;
    private long convertedCount;
    private long rejectedCount;
    private long waitlistedCount;
    private long cancelledCount;
}
