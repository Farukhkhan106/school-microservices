package com.successacademy.academicservice.service;

import com.successacademy.academicservice.client.FacultyServiceClient;
import com.successacademy.academicservice.client.StudentServiceClient;
import com.successacademy.academicservice.dto.*;
import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AcademicTaxonomyAndSeparationTest {

    // Official Assessment Mocks
    @Mock
    private AssessmentRepository assessmentRepository;

    @Mock
    private AcademicSessionRepository sessionRepository;

    @Mock
    private GradingSchemeRepository gradingSchemeRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AssessmentService assessmentService;

    // Academic Activity Mocks
    @Mock
    private AcademicActivityRepository activityRepository;

    @Mock
    private FacultyServiceClient facultyServiceClient;

    @Mock
    private StudentServiceClient studentServiceClient;

    @InjectMocks
    private AcademicActivityService activityService;

    // Activity Result Mocks
    @Mock
    private ActivityResultRepository resultRepository;

    @Mock
    private ActivitySubmissionRepository submissionRepository;

    @Mock
    private GradingService gradingService;

    @InjectMocks
    private ActivityResultService resultService;

    private AcademicSession activeSession;

    @BeforeEach
    void setUp() {
        activeSession = AcademicSession.builder()
                .id(1L)
                .sessionCode("2025-26")
                .name("Academic Session 2025-26")
                .isActive(true)
                .startDate(LocalDate.of(2025, 4, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .build();
    }

    // ── 1 to 5: ALLOWED OFFICIAL ASSESSMENT CREATION ───────────────────

    @Test
    @DisplayName("1. Periodic Assessment (UNIT_TEST) can be created as official assessment")
    void testOfficialPeriodicAssessmentCanBeCreated() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(i -> {
            Assessment a = i.getArgument(0);
            a.setId(10L);
            return a;
        });

        AssessmentRequest req = AssessmentRequest.builder()
                .sessionId(1L)
                .name("Periodic Assessment 1 (UT-1)")
                .assessmentType(AssessmentType.UNIT_TEST)
                .term("Term 1")
                .startDate(LocalDate.of(2025, 7, 10))
                .endDate(LocalDate.of(2025, 7, 18))
                .build();

        AssessmentResponse resp = assessmentService.createAssessment(req, 1L, "ADMIN");
        assertNotNull(resp);
        assertEquals(AssessmentType.UNIT_TEST, resp.getAssessmentType());
        assertEquals("Periodic Assessment (UT)", resp.getAssessmentTypeDisplayName());
    }

    @Test
    @DisplayName("2. Quarterly Examination (QUARTERLY) can be created as official assessment")
    void testOfficialQuarterlyCanBeCreated() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(i -> {
            Assessment a = i.getArgument(0);
            a.setId(11L);
            return a;
        });

        AssessmentRequest req = AssessmentRequest.builder()
                .sessionId(1L)
                .name("Quarterly Examination 2025")
                .assessmentType(AssessmentType.QUARTERLY)
                .term("Term 1")
                .startDate(LocalDate.of(2025, 8, 1))
                .endDate(LocalDate.of(2025, 8, 10))
                .build();

        AssessmentResponse resp = assessmentService.createAssessment(req, 1L, "ADMIN");
        assertNotNull(resp);
        assertEquals(AssessmentType.QUARTERLY, resp.getAssessmentType());
    }

    @Test
    @DisplayName("3. Half-Yearly / Mid-Term (HALF_YEARLY) can be created as official assessment")
    void testOfficialHalfYearlyCanBeCreated() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(i -> {
            Assessment a = i.getArgument(0);
            a.setId(12L);
            return a;
        });

        AssessmentRequest req = AssessmentRequest.builder()
                .sessionId(1L)
                .name("Term 1 / Half-Yearly Examination")
                .assessmentType(AssessmentType.HALF_YEARLY)
                .term("Term 1")
                .startDate(LocalDate.of(2025, 9, 15))
                .endDate(LocalDate.of(2025, 9, 28))
                .build();

        AssessmentResponse resp = assessmentService.createAssessment(req, 1L, "ADMIN");
        assertNotNull(resp);
        assertEquals(AssessmentType.HALF_YEARLY, resp.getAssessmentType());
    }

    @Test
    @DisplayName("4. Pre-Board Examination (PRE_BOARD) can be created as official assessment")
    void testOfficialPreBoardCanBeCreated() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(i -> {
            Assessment a = i.getArgument(0);
            a.setId(13L);
            return a;
        });

        AssessmentRequest req = AssessmentRequest.builder()
                .sessionId(1L)
                .name("Pre-Board Examination 2026")
                .assessmentType(AssessmentType.PRE_BOARD)
                .term("Term 2")
                .startDate(LocalDate.of(2026, 1, 10))
                .endDate(LocalDate.of(2026, 1, 25))
                .build();

        AssessmentResponse resp = assessmentService.createAssessment(req, 1L, "ADMIN");
        assertNotNull(resp);
        assertEquals(AssessmentType.PRE_BOARD, resp.getAssessmentType());
    }

    @Test
    @DisplayName("5. Annual / Final Examination (ANNUAL) can be created as official assessment")
    void testOfficialAnnualCanBeCreated() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(i -> {
            Assessment a = i.getArgument(0);
            a.setId(14L);
            return a;
        });

        AssessmentRequest req = AssessmentRequest.builder()
                .sessionId(1L)
                .name("Annual Final Examination 2026")
                .assessmentType(AssessmentType.ANNUAL)
                .term("Term 2")
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 20))
                .build();

        AssessmentResponse resp = assessmentService.createAssessment(req, 1L, "ADMIN");
        assertNotNull(resp);
        assertEquals(AssessmentType.ANNUAL, resp.getAssessmentType());
    }

    // ── 6 to 8: REJECTED NEW OFFICIAL ASSESSMENTS ────────────────────────

    @Test
    @DisplayName("6. New PERIOD_TEST is rejected with HTTP 400 Bad Request")
    void testNewPeriodTestIsRejected() {
        AssessmentRequest req = AssessmentRequest.builder()
                .sessionId(1L)
                .name("Period 3 Revision Test")
                .assessmentType(AssessmentType.PERIOD_TEST)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                assessmentService.createAssessment(req, 1L, "ADMIN"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid examination type"));
    }

    @Test
    @DisplayName("7. New WEEKLY_TEST is rejected with HTTP 400 Bad Request")
    void testNewWeeklyTestIsRejected() {
        AssessmentRequest req = AssessmentRequest.builder()
                .sessionId(1L)
                .name("Weekly Friday Test")
                .assessmentType(AssessmentType.WEEKLY_TEST)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                assessmentService.createAssessment(req, 1L, "ADMIN"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid examination type"));
    }

    @Test
    @DisplayName("8. New MONTHLY_TEST is rejected with HTTP 400 Bad Request")
    void testNewMonthlyTestIsRejected() {
        AssessmentRequest req = AssessmentRequest.builder()
                .sessionId(1L)
                .name("Monthly Assessment")
                .assessmentType(AssessmentType.MONTHLY_TEST)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                assessmentService.createAssessment(req, 1L, "ADMIN"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid examination type"));
    }

    // ── 9 to 12: HISTORICAL RECORD COMPATIBILITY ────────────────────────

    @Test
    @DisplayName("9. Existing historical PERIOD_TEST record can still be read successfully")
    void testHistoricalPeriodTestCanBeRead() {
        Assessment historical = Assessment.builder()
                .id(3L)
                .session(activeSession)
                .name("Historical Period 3 Test")
                .assessmentType(AssessmentType.PERIOD_TEST)
                .status(AssessmentStatus.PUBLISHED)
                .build();

        when(assessmentRepository.findById(3L)).thenReturn(Optional.of(historical));
        AssessmentResponse resp = assessmentService.getAssessmentById(3L);
        assertNotNull(resp);
        assertEquals(AssessmentType.PERIOD_TEST, resp.getAssessmentType());
        assertEquals("Period / In-Class Test", resp.getAssessmentTypeDisplayName());
    }

    @Test
    @DisplayName("10. Existing historical WEEKLY_TEST record can still be read successfully")
    void testHistoricalWeeklyTestCanBeRead() {
        Assessment historical = Assessment.builder()
                .id(4L)
                .session(activeSession)
                .name("Historical Weekly Test")
                .assessmentType(AssessmentType.WEEKLY_TEST)
                .status(AssessmentStatus.PUBLISHED)
                .build();

        when(assessmentRepository.findById(4L)).thenReturn(Optional.of(historical));
        AssessmentResponse resp = assessmentService.getAssessmentById(4L);
        assertNotNull(resp);
        assertEquals(AssessmentType.WEEKLY_TEST, resp.getAssessmentType());
        assertEquals("Weekly Test", resp.getAssessmentTypeDisplayName());
    }

    @Test
    @DisplayName("11. Existing historical MONTHLY_TEST record can still be read successfully")
    void testHistoricalMonthlyTestCanBeRead() {
        Assessment historical = Assessment.builder()
                .id(5L)
                .session(activeSession)
                .name("Historical Monthly Test")
                .assessmentType(AssessmentType.MONTHLY_TEST)
                .status(AssessmentStatus.SCHEDULED)
                .build();

        when(assessmentRepository.findById(5L)).thenReturn(Optional.of(historical));
        AssessmentResponse resp = assessmentService.getAssessmentById(5L);
        assertNotNull(resp);
        assertEquals(AssessmentType.MONTHLY_TEST, resp.getAssessmentType());
        assertEquals("Monthly Assessment", resp.getAssessmentTypeDisplayName());
    }

    @Test
    @DisplayName("12. Existing historical UNIT_TEST record can still be read successfully")
    void testHistoricalUnitTestCanBeRead() {
        Assessment historical = Assessment.builder()
                .id(2L)
                .session(activeSession)
                .name("Periodic Assessment 1 (UT-1)")
                .assessmentType(AssessmentType.UNIT_TEST)
                .status(AssessmentStatus.PUBLISHED)
                .build();

        when(assessmentRepository.findById(2L)).thenReturn(Optional.of(historical));
        AssessmentResponse resp = assessmentService.getAssessmentById(2L);
        assertNotNull(resp);
        assertEquals(AssessmentType.UNIT_TEST, resp.getAssessmentType());
        assertEquals("Periodic Assessment (UT)", resp.getAssessmentTypeDisplayName());
    }

    // ── 13 to 16: TEACHER CLASSROOM ACTIVITIES ─────────────────────────

    @Test
    @DisplayName("13. Teacher can create WEEKLY_TEST AcademicActivity")
    void testTeacherCreatesWeeklyTestActivity() {
        when(facultyServiceClient.isTeacherAssigned(101L, "10", "A", "Physics")).thenReturn(true);
        when(activityRepository.save(any(AcademicActivity.class))).thenAnswer(i -> {
            AcademicActivity a = i.getArgument(0);
            a.setId(101L);
            return a;
        });

        ActivityCreateRequest req = ActivityCreateRequest.builder()
                .title("Physics Weekly Quiz 1")
                .activityType(ActivityType.WEEKLY_TEST)
                .studentClass("10")
                .section("A")
                .subject("Physics")
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(2))
                .maxMarks(BigDecimal.valueOf(25))
                .build();

        ActivityResponse resp = activityService.createActivity(req, 101L, "farukh.khan", 555L, "TEACHER", false);
        assertNotNull(resp);
        assertEquals(ActivityType.WEEKLY_TEST, resp.getActivityType());
    }

    @Test
    @DisplayName("14. Teacher can create MONTHLY_TEST AcademicActivity")
    void testTeacherCreatesMonthlyTestActivity() {
        when(facultyServiceClient.isTeacherAssigned(101L, "10", "A", "Physics")).thenReturn(true);
        when(activityRepository.save(any(AcademicActivity.class))).thenAnswer(i -> {
            AcademicActivity a = i.getArgument(0);
            a.setId(102L);
            return a;
        });

        ActivityCreateRequest req = ActivityCreateRequest.builder()
                .title("Physics October Monthly Test")
                .activityType(ActivityType.MONTHLY_TEST)
                .studentClass("10")
                .section("A")
                .subject("Physics")
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(2))
                .maxMarks(BigDecimal.valueOf(50))
                .build();

        ActivityResponse resp = activityService.createActivity(req, 101L, "farukh.khan", 555L, "TEACHER", false);
        assertNotNull(resp);
        assertEquals(ActivityType.MONTHLY_TEST, resp.getActivityType());
    }

    @Test
    @DisplayName("15. Teacher can create UNIT_TEST AcademicActivity")
    void testTeacherCreatesUnitTestActivity() {
        when(facultyServiceClient.isTeacherAssigned(101L, "10", "A", "Physics")).thenReturn(true);
        when(activityRepository.save(any(AcademicActivity.class))).thenAnswer(i -> {
            AcademicActivity a = i.getArgument(0);
            a.setId(103L);
            return a;
        });

        ActivityCreateRequest req = ActivityCreateRequest.builder()
                .title("Unit 2 Kinematics Chapter Test")
                .activityType(ActivityType.UNIT_TEST)
                .studentClass("10")
                .section("A")
                .subject("Physics")
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(2))
                .maxMarks(BigDecimal.valueOf(30))
                .build();

        ActivityResponse resp = activityService.createActivity(req, 101L, "farukh.khan", 555L, "TEACHER", false);
        assertNotNull(resp);
        assertEquals(ActivityType.UNIT_TEST, resp.getActivityType());
    }

    @Test
    @DisplayName("16. Teacher can create SURPRISE_TEST AcademicActivity")
    void testTeacherCreatesSurpriseTestActivity() {
        when(facultyServiceClient.isTeacherAssigned(101L, "10", "A", "Physics")).thenReturn(true);
        when(activityRepository.save(any(AcademicActivity.class))).thenAnswer(i -> {
            AcademicActivity a = i.getArgument(0);
            a.setId(104L);
            return a;
        });

        ActivityCreateRequest req = ActivityCreateRequest.builder()
                .title("Surprise Concept Check on Optics")
                .activityType(ActivityType.SURPRISE_TEST)
                .studentClass("10")
                .section("A")
                .subject("Physics")
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(1))
                .maxMarks(BigDecimal.valueOf(10))
                .build();

        ActivityResponse resp = activityService.createActivity(req, 101L, "farukh.khan", 555L, "TEACHER", false);
        assertNotNull(resp);
        assertEquals(ActivityType.SURPRISE_TEST, resp.getActivityType());
    }

    // ── 17 to 20: SECURITY & ISOLATION CHECKS ──────────────────────────

    @Test
    @DisplayName("17. Activity results remain isolated in activity_results and do not write to student_marks")
    void testActivityResultsRemainIsolated() {
        AcademicActivity act = AcademicActivity.builder()
                .id(200L)
                .studentClass("10")
                .section("A")
                .subject("Physics")
                .teacherId(101L)
                .maxMarks(BigDecimal.valueOf(20))
                .passingMarks(BigDecimal.valueOf(7))
                .status(ActivityStatus.PUBLISHED)
                .build();

        when(activityRepository.findById(200L)).thenReturn(Optional.of(act));
        when(resultRepository.findByActivityIdAndStudentId(200L, 501L)).thenReturn(Optional.empty());
        when(studentServiceClient.getStudentById(501L)).thenReturn(
                StudentServiceClient.StudentInfoDto.builder()
                        .id(501L)
                        .firstName("Rohan")
                        .lastName("Verma")
                        .admissionNo("ADM-501")
                        .build()
        );
        when(resultRepository.save(any(ActivityResult.class))).thenAnswer(i -> {
            ActivityResult r = i.getArgument(0);
            r.setId(901L);
            return r;
        });

        BatchActivityResultRequest batchReq = BatchActivityResultRequest.builder()
                .entries(List.of(
                        ActivityResultEntryRequest.builder()
                                .studentId(501L)
                                .obtainedMarks(BigDecimal.valueOf(18))
                                .isAbsent(false)
                                .teacherFeedback("Great work!")
                                .build()
                ))
                .build();

        List<ActivityResultResponse> results = resultService.saveResultsBatch(200L, batchReq, 101L, 555L, "TEACHER", false);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(BigDecimal.valueOf(18), results.get(0).getObtainedMarks());
        // Verify saved to resultRepository (activity_results table)
        verify(resultRepository, times(1)).save(any(ActivityResult.class));
    }

    @Test
    @DisplayName("18. Teacher A cannot create activity for an unauthorized class/subject (HTTP 403)")
    void testTeacherCannotCreateActivityForUnauthorizedSubject() {
        when(facultyServiceClient.isTeacherAssigned(101L, "10", "A", "Chemistry")).thenReturn(false);

        ActivityCreateRequest req = ActivityCreateRequest.builder()
                .title("Chemistry Quiz")
                .activityType(ActivityType.WEEKLY_TEST)
                .studentClass("10")
                .section("A")
                .subject("Chemistry")
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(2))
                .maxMarks(BigDecimal.valueOf(25))
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                activityService.createActivity(req, 101L, "farukh.khan", 555L, "TEACHER", false));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("not assigned to teach Chemistry"));
    }

    @Test
    @DisplayName("19. Teacher A cannot modify Teacher B's activity (HTTP 403)")
    void testTeacherCannotModifyAnotherTeachersActivity() {
        AcademicActivity activityByTeacherB = AcademicActivity.builder()
                .id(300L)
                .teacherId(102L)
                .studentClass("10")
                .section("A")
                .subject("Biology")
                .status(ActivityStatus.DRAFT)
                .build();

        when(activityRepository.findById(300L)).thenReturn(Optional.of(activityByTeacherB));

        ActivityUpdateRequest updateReq = ActivityUpdateRequest.builder()
                .title("Attempted Title Hijack")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                activityService.updateActivity(300L, updateReq, 101L, 555L, "TEACHER", false));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You can only edit your own activities"));
    }

    @Test
    @DisplayName("20. Student can only view published activity results belonging to them")
    void testStudentCanOnlyViewOwnPublishedResults() {
        ActivityResult studentResult = ActivityResult.builder()
                .id(1L)
                .activityId(200L)
                .studentId(501L)
                .studentName("Rohan Verma")
                .status(ResultStatus.PUBLISHED)
                .obtainedMarks(BigDecimal.valueOf(19))
                .maxMarks(BigDecimal.valueOf(20))
                .percentage(BigDecimal.valueOf(95.0))
                .build();

        when(resultRepository.findByStudentIdAndStatus(501L, ResultStatus.PUBLISHED))
                .thenReturn(List.of(studentResult));

        List<ActivityResultResponse> myResults = resultService.getStudentPublishedResults(501L);
        assertNotNull(myResults);
        assertEquals(1, myResults.size());
        assertEquals(501L, myResults.get(0).getStudentId());
        assertEquals(ResultStatus.PUBLISHED, myResults.get(0).getStatus());
    }
}
