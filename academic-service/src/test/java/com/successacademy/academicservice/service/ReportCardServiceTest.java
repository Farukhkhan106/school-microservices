package com.successacademy.academicservice.service;

import com.successacademy.academicservice.client.AttendanceServiceClient;
import com.successacademy.academicservice.client.StudentServiceClient;
import com.successacademy.academicservice.dto.StudentReportCardResponse;
import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.AssessmentRepository;
import com.successacademy.academicservice.repository.StudentMarkRepository;
import com.successacademy.academicservice.repository.StudentResultSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportCardServiceTest {

    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private StudentMarkRepository markRepository;

    @Mock
    private StudentResultSummaryRepository summaryRepository;

    @Mock
    private StudentServiceClient studentServiceClient;

    @Mock
    private AttendanceServiceClient attendanceServiceClient;

    @InjectMocks
    private ReportCardService reportCardService;

    private Assessment assessment;
    private StudentMark mark;
    private AssessmentSchedule schedule;

    @BeforeEach
    void setUp() {
        AcademicSession session = AcademicSession.builder()
                .id(1L)
                .sessionCode("2025-2026")
                .name("Academic Session 2025-26")
                .startDate(LocalDate.of(2025, 4, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .build();

        assessment = Assessment.builder()
                .id(10L)
                .session(session)
                .name("Annual Examination 2026")
                .assessmentType(AssessmentType.ANNUAL)
                .status(AssessmentStatus.PUBLISHED)
                .isRankVisible(true)
                .build();

        schedule = AssessmentSchedule.builder()
                .id(100L)
                .assessment(assessment)
                .studentClass("10")
                .section("A")
                .subject("Mathematics")
                .maxMarks(BigDecimal.valueOf(100))
                .passMarks(BigDecimal.valueOf(33))
                .build();

        mark = StudentMark.builder()
                .id(1001L)
                .schedule(schedule)
                .studentId(501L)
                .studentName("Rahul Sharma")
                .rollNo("10A-01")
                .marksObtained(BigDecimal.valueOf(92))
                .isAbsent(false)
                .grade("A1")
                .isPassing(true)
                .build();
    }

    @Test
    @DisplayName("Report Card must use real student admission number and real attendance summary")
    void testGetReportCard_UsesRealStudentAndAttendanceData() {
        Long assessmentId = 10L;
        Long studentId = 501L;

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(markRepository.findByAssessmentIdAndStudentId(assessmentId, studentId)).thenReturn(List.of(mark));
        when(summaryRepository.findByAssessmentIdAndStudentId(assessmentId, studentId)).thenReturn(Optional.empty());

        // Mock real student info
        StudentServiceClient.StudentInfoDto studentInfo = StudentServiceClient.StudentInfoDto.builder()
                .id(studentId)
                .admissionNo("ADM-2024-9876")
                .firstName("Rahul")
                .lastName("Sharma")
                .studentClass("10")
                .section("A")
                .build();
        when(studentServiceClient.getStudentById(studentId)).thenReturn(studentInfo);

        // Mock real attendance summary from attendance-service
        AttendanceServiceClient.AttendanceSummaryDto attSummary = AttendanceServiceClient.AttendanceSummaryDto.builder()
                .studentId(studentId)
                .totalDays(180)
                .presentDays(171)
                .absentDays(9)
                .overallPercentage(95.0)
                .build();
        when(attendanceServiceClient.getStudentAttendanceSummary(studentId)).thenReturn(attSummary);

        StudentReportCardResponse response = reportCardService.getReportCard(assessmentId, studentId, false);

        assertNotNull(response);
        assertEquals("ADM-2024-9876", response.getAdmissionNo(), "Must use real admission number, never hardcoded SA-{id}");
        assertEquals(180, response.getTotalWorkingDays(), "Must use real total days from attendance-service");
        assertEquals(171, response.getDaysPresent(), "Must use real present days from attendance-service");
        assertEquals(BigDecimal.valueOf(95.0).setScale(1), response.getAttendancePercentage());
    }

    @Test
    @DisplayName("Report Card handles external service failure gracefully without hardcoded fake data")
    void testGetReportCard_HandlesServiceFailureGracefully() {
        Long assessmentId = 10L;
        Long studentId = 501L;

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(markRepository.findByAssessmentIdAndStudentId(assessmentId, studentId)).thenReturn(List.of(mark));
        when(summaryRepository.findByAssessmentIdAndStudentId(assessmentId, studentId)).thenReturn(Optional.empty());

        // When external services fail or return null
        when(studentServiceClient.getStudentById(studentId)).thenReturn(null);
        when(attendanceServiceClient.getStudentAttendanceSummary(studentId)).thenReturn(null);

        StudentReportCardResponse response = reportCardService.getReportCard(assessmentId, studentId, false);

        assertNotNull(response);
        // Falls back to roll number instead of fake SA-{id}
        assertEquals("10A-01", response.getAdmissionNo());
        // Defaults to 0 instead of fake hardcoded 120 / 106 / 88.5%
        assertEquals(0, response.getTotalWorkingDays());
        assertEquals(0, response.getDaysPresent());
        assertEquals(BigDecimal.ZERO, response.getAttendancePercentage());
    }
}
