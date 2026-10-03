package com.successacademy.academicservice.service;

import com.successacademy.academicservice.dto.ClassPerformanceAnalyticsResponse;
import com.successacademy.academicservice.dto.StudentRankDto;
import com.successacademy.academicservice.dto.SubjectPerformanceDto;
import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.AssessmentRepository;
import com.successacademy.academicservice.repository.AssessmentScheduleRepository;
import com.successacademy.academicservice.repository.StudentMarkRepository;
import com.successacademy.academicservice.repository.StudentResultSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository scheduleRepository;
    private final StudentMarkRepository markRepository;
    private final StudentResultSummaryRepository summaryRepository;

    public ClassPerformanceAnalyticsResponse getClassAnalytics(Long assessmentId, String studentClass, String section) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found with ID: " + assessmentId));

        List<StudentResultSummary> summaries = summaryRepository
                .findByAssessmentIdAndStudentClassAndSectionOrderByPercentageDesc(assessmentId, studentClass, section);

        int totalStrength = summaries.size();
        int passed = 0;
        int failed = 0;
        BigDecimal sumPercentages = BigDecimal.ZERO;
        BigDecimal highestPct = BigDecimal.ZERO;
        String topperName = null;
        BigDecimal lowestPct = totalStrength > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;

        List<StudentRankDto> rankDtos = new ArrayList<>();
        List<StudentRankDto> remedial = new ArrayList<>();

        for (StudentResultSummary s : summaries) {
            boolean isPassed = "PASSED".equalsIgnoreCase(s.getOverallResult());
            if (isPassed) passed++;
            else failed++;

            sumPercentages = sumPercentages.add(s.getPercentage());

            if (s.getPercentage().compareTo(highestPct) > 0) {
                highestPct = s.getPercentage();
                topperName = s.getStudentName();
            }

            if (s.getPercentage().compareTo(lowestPct) < 0) {
                lowestPct = s.getPercentage();
            }

            StudentRankDto rankDto = StudentRankDto.builder()
                    .studentId(s.getStudentId())
                    .studentName(s.getStudentName())
                    .rollNo(s.getRollNo())
                    .studentClass(s.getStudentClass())
                    .section(s.getSection())
                    .totalMarksObtained(s.getTotalMarksObtained())
                    .totalMaxMarks(s.getTotalMaxMarks())
                    .percentage(s.getPercentage())
                    .overallGrade(s.getOverallGrade())
                    .overallResult(s.getOverallResult())
                    .rankInSection(assessment.isRankVisible() ? s.getRankInSection() : null)
                    .rankInClass(assessment.isRankVisible() ? s.getRankInClass() : null)
                    .build();

            rankDtos.add(rankDto);

            // Remedial watch: scored below 33% or failed
            if (!isPassed || s.getPercentage().compareTo(BigDecimal.valueOf(33.0)) < 0) {
                remedial.add(rankDto);
            }
        }

        BigDecimal passPct = totalStrength > 0
                ? BigDecimal.valueOf(passed).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalStrength), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal classAverage = totalStrength > 0
                ? sumPercentages.divide(BigDecimal.valueOf(totalStrength), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Subject breakdown
        List<AssessmentSchedule> schedules = scheduleRepository.findByAssessmentIdAndStudentClassAndSection(assessmentId, studentClass, section);
        List<SubjectPerformanceDto> subjectDtos = new ArrayList<>();

        for (AssessmentSchedule sch : schedules) {
            List<StudentMark> marks = markRepository.findByScheduleIdOrderByRollNoAsc(sch.getId());
            int appeared = 0;
            int subjPassed = 0;
            BigDecimal subjSum = BigDecimal.ZERO;
            BigDecimal subjHighest = BigDecimal.ZERO;
            BigDecimal subjLowest = sch.getMaxMarks();

            for (StudentMark m : marks) {
                if (!m.isAbsent() && m.getMarksObtained() != null) {
                    appeared++;
                    subjSum = subjSum.add(m.getMarksObtained());
                    if (m.isPassing()) subjPassed++;
                    if (m.getMarksObtained().compareTo(subjHighest) > 0) subjHighest = m.getMarksObtained();
                    if (m.getMarksObtained().compareTo(subjLowest) < 0) subjLowest = m.getMarksObtained();
                }
            }

            BigDecimal subjAvgPct = (appeared > 0 && sch.getMaxMarks().compareTo(BigDecimal.ZERO) > 0)
                    ? subjSum.divide(BigDecimal.valueOf(appeared), 2, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).divide(sch.getMaxMarks(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal subjPassPct = appeared > 0
                    ? BigDecimal.valueOf(subjPassed).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(appeared), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            subjectDtos.add(SubjectPerformanceDto.builder()
                    .subject(sch.getSubject())
                    .averagePercentage(subjAvgPct)
                    .highestMarks(subjHighest)
                    .lowestMarks(appeared > 0 ? subjLowest : BigDecimal.ZERO)
                    .maxMarks(sch.getMaxMarks())
                    .appearedCount(appeared)
                    .passedCount(subjPassed)
                    .passPercentage(subjPassPct)
                    .build());
        }

        return ClassPerformanceAnalyticsResponse.builder()
                .assessmentId(assessment.getId())
                .assessmentName(assessment.getName())
                .studentClass(studentClass)
                .section(section)
                .totalStrength(totalStrength)
                .appearedStudents(totalStrength)
                .absentStudents(0)
                .passedStudents(passed)
                .failedStudents(failed)
                .passPercentage(passPct)
                .classAveragePercentage(classAverage)
                .highestPercentage(highestPct)
                .classTopperName(topperName != null ? topperName : "N/A")
                .lowestPercentage(totalStrength > 0 ? lowestPct : BigDecimal.ZERO)
                .subjects(subjectDtos)
                .rankings(rankDtos)
                .remedialStudents(remedial)
                .build();
    }
}
