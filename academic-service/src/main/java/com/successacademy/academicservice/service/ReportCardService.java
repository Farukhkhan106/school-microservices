package com.successacademy.academicservice.service;

import com.successacademy.academicservice.dto.StudentReportCardResponse;
import com.successacademy.academicservice.dto.SubjectResultDto;
import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.AssessmentRepository;
import com.successacademy.academicservice.repository.StudentMarkRepository;
import com.successacademy.academicservice.repository.StudentResultSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportCardService {

    private final AssessmentRepository assessmentRepository;
    private final StudentMarkRepository markRepository;
    private final StudentResultSummaryRepository summaryRepository;
    private final com.successacademy.academicservice.client.StudentServiceClient studentServiceClient;
    private final com.successacademy.academicservice.client.AttendanceServiceClient attendanceServiceClient;

    public StudentReportCardResponse getReportCard(Long assessmentId, Long studentId, boolean isStudentOrParent) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found with ID: " + assessmentId));

        // If accessed by Student or Parent, assessment MUST be PUBLISHED
        if (isStudentOrParent && assessment.getStatus() != AssessmentStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Result for this assessment has not been officially published yet.");
        }

        List<StudentMark> marks = markRepository.findByAssessmentIdAndStudentId(assessmentId, studentId);
        if (marks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No assessment records found for this student.");
        }

        StudentMark sample = marks.get(0);
        StudentResultSummary summary = summaryRepository.findByAssessmentIdAndStudentId(assessmentId, studentId).orElse(null);

        List<SubjectResultDto> subjectDtos = new ArrayList<>();
        BigDecimal totalObt = BigDecimal.ZERO;
        BigDecimal totalMax = BigDecimal.ZERO;

        for (StudentMark m : marks) {
            AssessmentSchedule sch = m.getSchedule();
            totalMax = totalMax.add(sch.getMaxMarks());
            if (!m.isAbsent() && m.getMarksObtained() != null) {
                totalObt = totalObt.add(m.getMarksObtained());
            }

            subjectDtos.add(SubjectResultDto.builder()
                    .subject(sch.getSubject())
                    .component(sch.getComponent())
                    .marksObtained(m.getMarksObtained())
                    .maxMarks(sch.getMaxMarks())
                    .passMarks(sch.getPassMarks())
                    .grade(m.getGrade())
                    .gradePoint(m.getGradePoint())
                    .isPassing(m.isPassing())
                    .isAbsent(m.isAbsent())
                    .remarks(m.getRemarks())
                    .teacherName(sch.getTeacherName())
                    .build());
        }

        BigDecimal percentage = summary != null ? summary.getPercentage() : BigDecimal.ZERO;
        String overallGrade = summary != null ? summary.getOverallGrade() : "E";
        String overallResult = summary != null ? summary.getOverallResult() : "PENDING";
        Integer rank = (summary != null && assessment.isRankVisible()) ? summary.getRankInSection() : null;

        String classTeacherRemarks = generateConstructiveRemark(overallGrade);

        // Retrieve real student admission number
        var studentInfo = studentServiceClient.getStudentById(studentId);
        String realAdmissionNo = (studentInfo != null && studentInfo.getAdmissionNo() != null && !studentInfo.getAdmissionNo().isBlank())
                ? studentInfo.getAdmissionNo().trim()
                : (sample.getRollNo() != null ? sample.getRollNo() : String.valueOf(studentId));

        // Retrieve real attendance statistics from attendance-service
        var attSummary = attendanceServiceClient.getStudentAttendanceSummary(studentId);
        BigDecimal realAttendancePercentage = (attSummary != null && attSummary.getTotalDays() > 0)
                ? BigDecimal.valueOf(attSummary.getOverallPercentage()).setScale(1, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        int totalWorkingDays = attSummary != null ? (int) attSummary.getTotalDays() : 0;
        int daysPresent = attSummary != null ? (int) attSummary.getPresentDays() : 0;

        return StudentReportCardResponse.builder()
                .schoolName("Success Academy English Medium Higher Secondary School")
                .schoolAffiliation("CBSE Pattern Curriculum • Affiliation Code: 1030842 • School Code: 50807")
                .schoolAddress("Main Campus, Ajanas Road, Satwas, Dist. Dewas (M.P.) - 455459")
                .studentId(studentId)
                .studentName(sample.getStudentName())
                .admissionNo(realAdmissionNo)
                .rollNo(sample.getRollNo())
                .studentClass(sample.getSchedule().getStudentClass())
                .section(sample.getSchedule().getSection())
                .sessionCode(assessment.getSession().getSessionCode())
                .sessionName(assessment.getSession().getName())
                .assessmentId(assessment.getId())
                .assessmentName(assessment.getName())
                .assessmentType(assessment.getAssessmentType().getDisplayName())
                .term(assessment.getTerm())
                .reportIssueDate(LocalDate.now())
                .attendancePercentage(realAttendancePercentage)
                .totalWorkingDays(totalWorkingDays)
                .daysPresent(daysPresent)
                .subjects(subjectDtos)
                .totalMarksObtained(totalObt)
                .totalMaxMarks(totalMax)
                .overallPercentage(percentage)
                .overallGrade(overallGrade)
                .overallResult(overallResult)
                .rankInSection(rank)
                .rankInClass(rank)
                .classTeacherRemarks(classTeacherRemarks)
                .principalRemarks("Good academic trajectory. Recommended for advanced curriculum enrichment.")
                .build();
    }

    private String generateConstructiveRemark(String grade) {
        if ("A1".equalsIgnoreCase(grade)) return "Exceptional academic performance! Demonstrated mastery across all scholastic domains.";
        if ("A2".equalsIgnoreCase(grade)) return "Excellent consistency and analytical understanding. Commendable achievement.";
        if ("B1".equalsIgnoreCase(grade)) return "Very good grasp of subject fundamentals. Regular revision will push scores to top tier.";
        if ("B2".equalsIgnoreCase(grade)) return "Good performance. Encouraged to participate more actively in classroom problem solving.";
        if ("C1".equalsIgnoreCase(grade) || "C2".equalsIgnoreCase(grade)) return "Average foundational comprehension. Needs targeted practice in core numeric and reasoning concepts.";
        if ("D".equalsIgnoreCase(grade)) return "Passed benchmark. Essential remedial assistance recommended to strengthen weak subject areas.";
        return "Needs urgent academic improvement. Personalized remedial plan and parent consultation initiated.";
    }
}
