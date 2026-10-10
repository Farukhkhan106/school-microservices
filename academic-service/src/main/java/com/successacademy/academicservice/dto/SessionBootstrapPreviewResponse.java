package com.successacademy.academicservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionBootstrapPreviewResponse {
    private String tenantId;
    private Long activeSessionId;
    private String activeSessionCode;
    private String activeSessionName;

    // Student metrics
    private int totalMasterStudents;
    private int activeMasterStudents;
    private int inactiveMasterStudents;
    private int alreadyEnrolledCount;
    private int pendingEnrollmentCount;
    private int invalidPlacementCount;
    private int ambiguousCount;

    // Faculty & teaching structure metrics
    private int totalFaculty;
    private int activeFaculty;
    private int totalTeachingAssignments;
    private int totalClassSchedules;

    // Academic structure metrics
    private int totalDistinctClasses;
    private int totalDistinctSections;

    // Assessment & operational metrics
    private int historicalAssessmentsCount;
    private int currentSessionAssessmentsCount;
    private int totalAttendanceRecords;
    private int totalFeeRecords;

    private List<String> warnings;
}
