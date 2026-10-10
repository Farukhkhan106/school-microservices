package com.successacademy.academicservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionBootstrapExecuteResponse {
    private String tenantId;
    private Long targetSessionId;
    private String targetSessionCode;
    private String targetSessionName;

    private int totalMasterStudents;
    private int activeStudentsEvaluated;
    private int inactiveStudentsSkipped;
    private int alreadyEnrolledPreserved;
    private int newlyEnrolledCreated;
    private int finalSessionEnrollmentCount;

    private int ambiguousSkipped;
    private List<String> skippedReasons;
    private LocalDateTime executedAt;
    private String status;
}
