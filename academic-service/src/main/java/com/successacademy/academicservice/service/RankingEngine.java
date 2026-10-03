package com.successacademy.academicservice.service;

import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.AssessmentScheduleRepository;
import com.successacademy.academicservice.repository.StudentMarkRepository;
import com.successacademy.academicservice.repository.StudentResultSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankingEngine {

    private final StudentMarkRepository markRepository;
    private final AssessmentScheduleRepository scheduleRepository;
    private final StudentResultSummaryRepository summaryRepository;
    private final GradingService gradingService;

    @Transactional
    public void calculateRanksForClassAndSection(Long assessmentId, String studentClass, String section) {
        List<StudentMark> allMarks = markRepository.findByAssessmentIdAndClassAndSection(assessmentId, studentClass, section);
        if (allMarks.isEmpty()) return;

        // Group marks by studentId
        Map<Long, List<StudentMark>> marksByStudent = new HashMap<>();
        Map<Long, StudentMark> sampleMarkByStudent = new HashMap<>();

        for (StudentMark m : allMarks) {
            marksByStudent.computeIfAbsent(m.getStudentId(), k -> new ArrayList<>()).add(m);
            sampleMarkByStudent.putIfAbsent(m.getStudentId(), m);
        }

        List<StudentResultSummary> summaries = new ArrayList<>();

        for (Map.Entry<Long, List<StudentMark>> entry : marksByStudent.entrySet()) {
            Long studentId = entry.getKey();
            List<StudentMark> studentMarks = entry.getValue();
            StudentMark sample = sampleMarkByStudent.get(studentId);

            BigDecimal totalObtained = BigDecimal.ZERO;
            BigDecimal totalMax = BigDecimal.ZERO;
            boolean hasFailedSubject = false;

            for (StudentMark sm : studentMarks) {
                BigDecimal max = sm.getSchedule().getMaxMarks();
                totalMax = totalMax.add(max != null ? max : BigDecimal.ZERO);

                if (sm.isAbsent()) {
                    hasFailedSubject = true;
                } else if (sm.getMarksObtained() != null) {
                    totalObtained = totalObtained.add(sm.getMarksObtained());
                    if (!sm.isPassing()) {
                        hasFailedSubject = true;
                    }
                } else {
                    hasFailedSubject = true;
                }
            }

            BigDecimal percentage = BigDecimal.ZERO;
            if (totalMax.compareTo(BigDecimal.ZERO) > 0) {
                percentage = totalObtained.multiply(BigDecimal.valueOf(100)).divide(totalMax, 2, RoundingMode.HALF_UP);
            }

            GradingRule overallRule = gradingService.calculateGrade(percentage, sample.getSchedule().getAssessment().getGradingScheme());
            String overallGrade = overallRule != null ? overallRule.getGrade() : "E";
            String overallResult = hasFailedSubject ? "FAILED" : "PASSED";

            StudentResultSummary summary = summaryRepository.findByAssessmentIdAndStudentId(assessmentId, studentId)
                    .orElse(StudentResultSummary.builder()
                            .sessionId(sample.getSchedule().getAssessment().getSession().getId())
                            .assessmentId(assessmentId)
                            .studentId(studentId)
                            .studentName(sample.getStudentName())
                            .rollNo(sample.getRollNo())
                            .studentClass(studentClass)
                            .section(section)
                            .build());

            summary.setTotalMarksObtained(totalObtained);
            summary.setTotalMaxMarks(totalMax);
            summary.setPercentage(percentage);
            summary.setOverallGrade(overallGrade);
            summary.setOverallResult(overallResult);

            summaries.add(summary);
        }

        // Sort summaries by percentage descending, then totalObtained descending
        summaries.sort((a, b) -> {
            int cmp = b.getPercentage().compareTo(a.getPercentage());
            if (cmp != 0) return cmp;
            return b.getTotalMarksObtained().compareTo(a.getTotalMarksObtained());
        });

        // Assign standard competition ranks (1, 2, 2, 4...)
        int currentRank = 1;
        for (int i = 0; i < summaries.size(); i++) {
            if (i > 0) {
                StudentResultSummary prev = summaries.get(i - 1);
                StudentResultSummary curr = summaries.get(i);
                if (curr.getPercentage().compareTo(prev.getPercentage()) == 0 &&
                    curr.getTotalMarksObtained().compareTo(prev.getTotalMarksObtained()) == 0) {
                    curr.setRankInSection(prev.getRankInSection());
                } else {
                    curr.setRankInSection(i + 1);
                }
            } else {
                summaries.get(0).setRankInSection(1);
            }
        }

        summaryRepository.saveAll(summaries);
        log.info("Calculated rankings for Assessment: {}, Class: {}-{}, Count: {}", assessmentId, studentClass, section, summaries.size());
    }
}
