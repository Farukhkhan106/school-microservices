package com.successacademy.academicservice.config;

import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.*;
import com.successacademy.academicservice.service.RankingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AcademicSessionRepository sessionRepository;
    private final GradingSchemeRepository gradingSchemeRepository;
    private final GradingRuleRepository gradingRuleRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentScheduleRepository scheduleRepository;
    private final StudentMarkRepository markRepository;
    private final RankingEngine rankingEngine;

    @Override
    public void run(String... args) {
        if (sessionRepository.count() > 0) {
            log.info("Academic data already initialized. Skipping seed.");
            return;
        }

        log.info("Initializing comprehensive Academic, Examination and Grading data...");

        // 1. ACADEMIC SESSIONS
        AcademicSession session2425 = sessionRepository.save(AcademicSession.builder()
                .sessionCode("2024-2025")
                .name("Academic Session 2024-25")
                .startDate(LocalDate.of(2024, 4, 1))
                .endDate(LocalDate.of(2025, 3, 31))
                .isActive(false)
                .description("Previous Academic Year")
                .build());

        AcademicSession session2526 = sessionRepository.save(AcademicSession.builder()
                .sessionCode("2025-2026")
                .name("Academic Session 2025-26")
                .startDate(LocalDate.of(2025, 4, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .isActive(true)
                .description("Current Active Academic Session")
                .build());

        // 2. CBSE 9-POINT GRADING SCHEME
        GradingScheme cbseScheme = GradingScheme.builder()
                .name("CBSE 9-Point Scale")
                .description("Standard Central Board of Secondary Education Continuous and Comprehensive Evaluation scale")
                .isDefault(true)
                .build();
        cbseScheme = gradingSchemeRepository.save(cbseScheme);

        List<GradingRule> rules = List.of(
                GradingRule.builder().scheme(cbseScheme).grade("A1").minPercentage(BigDecimal.valueOf(91.00)).maxPercentage(BigDecimal.valueOf(100.00)).gradePoint(BigDecimal.valueOf(10.0)).isPassing(true).description("Outstanding").build(),
                GradingRule.builder().scheme(cbseScheme).grade("A2").minPercentage(BigDecimal.valueOf(81.00)).maxPercentage(BigDecimal.valueOf(90.99)).gradePoint(BigDecimal.valueOf(9.0)).isPassing(true).description("Excellent").build(),
                GradingRule.builder().scheme(cbseScheme).grade("B1").minPercentage(BigDecimal.valueOf(71.00)).maxPercentage(BigDecimal.valueOf(80.99)).gradePoint(BigDecimal.valueOf(8.0)).isPassing(true).description("Very Good").build(),
                GradingRule.builder().scheme(cbseScheme).grade("B2").minPercentage(BigDecimal.valueOf(61.00)).maxPercentage(BigDecimal.valueOf(70.99)).gradePoint(BigDecimal.valueOf(7.0)).isPassing(true).description("Good").build(),
                GradingRule.builder().scheme(cbseScheme).grade("C1").minPercentage(BigDecimal.valueOf(51.00)).maxPercentage(BigDecimal.valueOf(60.99)).gradePoint(BigDecimal.valueOf(6.0)).isPassing(true).description("Above Average").build(),
                GradingRule.builder().scheme(cbseScheme).grade("C2").minPercentage(BigDecimal.valueOf(41.00)).maxPercentage(BigDecimal.valueOf(50.99)).gradePoint(BigDecimal.valueOf(5.0)).isPassing(true).description("Average").build(),
                GradingRule.builder().scheme(cbseScheme).grade("D").minPercentage(BigDecimal.valueOf(33.00)).maxPercentage(BigDecimal.valueOf(40.99)).gradePoint(BigDecimal.valueOf(4.0)).isPassing(true).description("Pass Benchmark").build(),
                GradingRule.builder().scheme(cbseScheme).grade("E").minPercentage(BigDecimal.valueOf(0.00)).maxPercentage(BigDecimal.valueOf(32.99)).gradePoint(BigDecimal.valueOf(0.0)).isPassing(false).description("Needs Improvement / Essential Repeat").build()
        );
        gradingRuleRepository.saveAll(rules);

        // 3. ASSESSMENTS
        Assessment halfYearly = assessmentRepository.save(Assessment.builder()
                .session(session2526)
                .name("Term 1 / Half-Yearly Examination 2025-26")
                .assessmentType(AssessmentType.HALF_YEARLY)
                .term("Term 1")
                .startDate(LocalDate.of(2025, 9, 15))
                .endDate(LocalDate.of(2025, 9, 28))
                .gradingScheme(cbseScheme)
                .isRankVisible(true)
                .status(AssessmentStatus.PUBLISHED)
                .createdBy(1L)
                .description("Mid-session comprehensive evaluation for Classes 6 through 12")
                .build());

        Assessment unitTest1 = assessmentRepository.save(Assessment.builder()
                .session(session2526)
                .name("Periodic Assessment 1 (UT-1)")
                .assessmentType(AssessmentType.UNIT_TEST)
                .term("Term 1")
                .startDate(LocalDate.of(2025, 7, 10))
                .endDate(LocalDate.of(2025, 7, 18))
                .gradingScheme(cbseScheme)
                .isRankVisible(true)
                .status(AssessmentStatus.EVALUATION)
                .createdBy(1L)
                .description("First periodic assessment testing foundational syllabus units")
                .build());



        // 4. ASSESSMENT SCHEDULES FOR HALF-YEARLY (CLASS 10-A)
        AssessmentSchedule schMath = scheduleRepository.save(AssessmentSchedule.builder()
                .assessment(halfYearly)
                .studentClass("10")
                .section("A")
                .subject("Mathematics")
                .component(AssessmentComponent.THEORY)
                .examDate(LocalDate.of(2025, 9, 16))
                .periodNo(null)
                .teacherId(12L) // Mr. Rajesh Sharma
                .teacherName("Mr. Rajesh Sharma")
                .maxMarks(BigDecimal.valueOf(80.00))
                .passMarks(BigDecimal.valueOf(27.00))
                .status(MarkStatus.VERIFIED)
                .build());

        AssessmentSchedule schSci = scheduleRepository.save(AssessmentSchedule.builder()
                .assessment(halfYearly)
                .studentClass("10")
                .section("A")
                .subject("Science")
                .component(AssessmentComponent.THEORY)
                .examDate(LocalDate.of(2025, 9, 18))
                .periodNo(null)
                .teacherId(14L) // Mrs. Priya Verma
                .teacherName("Mrs. Priya Verma")
                .maxMarks(BigDecimal.valueOf(80.00))
                .passMarks(BigDecimal.valueOf(27.00))
                .status(MarkStatus.VERIFIED)
                .build());

        AssessmentSchedule schEng = scheduleRepository.save(AssessmentSchedule.builder()
                .assessment(halfYearly)
                .studentClass("10")
                .section("A")
                .subject("English Literature")
                .component(AssessmentComponent.THEORY)
                .examDate(LocalDate.of(2025, 9, 20))
                .periodNo(null)
                .teacherId(12L)
                .teacherName("Mr. Rajesh Sharma")
                .maxMarks(BigDecimal.valueOf(80.00))
                .passMarks(BigDecimal.valueOf(27.00))
                .status(MarkStatus.VERIFIED)
                .build());

        AssessmentSchedule schSoc = scheduleRepository.save(AssessmentSchedule.builder()
                .assessment(halfYearly)
                .studentClass("10")
                .section("A")
                .subject("Social Science")
                .component(AssessmentComponent.THEORY)
                .examDate(LocalDate.of(2025, 9, 22))
                .periodNo(null)
                .teacherId(16L) // Mr. Amit Tiwari
                .teacherName("Mr. Amit Tiwari")
                .maxMarks(BigDecimal.valueOf(80.00))
                .passMarks(BigDecimal.valueOf(27.00))
                .status(MarkStatus.VERIFIED)
                .build());

        AssessmentSchedule schHin = scheduleRepository.save(AssessmentSchedule.builder()
                .assessment(halfYearly)
                .studentClass("10")
                .section("A")
                .subject("Hindi Course-A")
                .component(AssessmentComponent.THEORY)
                .examDate(LocalDate.of(2025, 9, 24))
                .periodNo(null)
                .teacherId(14L)
                .teacherName("Mrs. Priya Verma")
                .maxMarks(BigDecimal.valueOf(80.00))
                .passMarks(BigDecimal.valueOf(27.00))
                .status(MarkStatus.VERIFIED)
                .build());



        // 5. SEED STUDENT MARKS FOR CLASS 10-A
        // Students: 55 (Rahul Sharma), 56 (Ananya Patel), 57 (Aarav Joshi), 58 (Sneha Gupta), 59 (Kabir Verma)
        Object[][] studentData = {
                {55L, "Rahul Sharma", "101", 76.0, 72.0, 70.0, 74.0, 75.0, "Outstanding conceptual grasp and analytical accuracy."},
                {56L, "Ananya Patel", "102", 78.0, 75.0, 74.0, 76.0, 77.0, "Consistent excellence and exceptional presentation."},
                {57L, "Aarav Joshi",  "103", 64.0, 58.0, 62.0, 60.0, 65.0, "Good performance. Can excel further with daily problem practice."},
                {58L, "Sneha Gupta",  "104", 70.0, 68.0, 72.0, 69.0, 71.0, "Very good command of core formulas and language syntax."},
                {59L, "Kabir Verma",  "105", 28.0, 32.0, 35.0, 30.0, 34.0, "Remedial support recommended in numeracy and science fundamentals."}
        };

        List<StudentMark> marksToSave = new ArrayList<>();
        AssessmentSchedule[] schedules = {schMath, schSci, schEng, schSoc, schHin};

        for (Object[] s : studentData) {
            Long sId = (Long) s[0];
            String name = (String) s[1];
            String roll = (String) s[2];
            String remark = (String) s[8];

            for (int i = 0; i < schedules.length; i++) {
                AssessmentSchedule sch = schedules[i];
                Double markVal = (Double) s[3 + i];
                BigDecimal markBd = BigDecimal.valueOf(markVal);

                // calculate grade
                double pct = (markVal / 80.0) * 100.0;
                String grade = "E";
                double gp = 0.0;
                boolean pass = markVal >= 27.0;

                if (pct >= 91.0) { grade = "A1"; gp = 10.0; }
                else if (pct >= 81.0) { grade = "A2"; gp = 9.0; }
                else if (pct >= 71.0) { grade = "B1"; gp = 8.0; }
                else if (pct >= 61.0) { grade = "B2"; gp = 7.0; }
                else if (pct >= 51.0) { grade = "C1"; gp = 6.0; }
                else if (pct >= 41.0) { grade = "C2"; gp = 5.0; }
                else if (pct >= 33.0) { grade = "D"; gp = 4.0; }

                marksToSave.add(StudentMark.builder()
                        .schedule(sch)
                        .studentId(sId)
                        .studentName(name)
                        .rollNo(roll)
                        .marksObtained(markBd)
                        .isAbsent(false)
                        .grade(grade)
                        .gradePoint(BigDecimal.valueOf(gp))
                        .isPassing(pass)
                        .remarks(remark)
                        .enteredBy(sch.getTeacherId())
                        .status(MarkStatus.VERIFIED)
                        .build());
            }
        }

        markRepository.saveAll(marksToSave);

        // 6. CALCULATE RANKINGS & SUMMARIES
        rankingEngine.calculateRanksForClassAndSection(halfYearly.getId(), "10", "A");

        log.info("Academic data initialization complete! Seeded Session, CBSE Grading Scheme, Assessments, Schedules, and Student Marks.");
    }
}
