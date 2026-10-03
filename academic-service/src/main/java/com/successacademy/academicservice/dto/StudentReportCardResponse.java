package com.successacademy.academicservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentReportCardResponse {
    // School Details
    private String schoolName;
    private String schoolAffiliation;
    private String schoolAddress;

    // Student & Academic Details
    private Long studentId;
    private String studentName;
    private String admissionNo;
    private String rollNo;
    private String studentClass;
    private String section;
    private String sessionCode;
    private String sessionName;
    private Long assessmentId;
    private String assessmentName;
    private String assessmentType;
    private String term;
    private LocalDate reportIssueDate;

    // Attendance
    private BigDecimal attendancePercentage;
    private Integer totalWorkingDays;
    private Integer daysPresent;

    // Scholastic Performance
    private List<SubjectResultDto> subjects;
    private BigDecimal totalMarksObtained;
    private BigDecimal totalMaxMarks;
    private BigDecimal overallPercentage;
    private String overallGrade;
    private String overallResult; // "PASSED", "PROMOTED", "ESSENTIAL_REPEAT"
    private Integer rankInSection; // null if ranking hidden
    private Integer rankInClass;   // null if ranking hidden

    // Remarks & Signatures
    private String classTeacherRemarks;
    private String principalRemarks;
}
