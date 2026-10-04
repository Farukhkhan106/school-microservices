package com.successacademy.academicservice.service;

import com.successacademy.academicservice.client.StudentServiceClient;
import com.successacademy.academicservice.dto.*;
import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.AcademicActivityRepository;
import com.successacademy.academicservice.repository.ActivityResultRepository;
import com.successacademy.academicservice.repository.ActivitySubmissionRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivitySubmissionAndResultServiceTest {

    @Mock
    private ActivitySubmissionRepository submissionRepository;

    @Mock
    private ActivityResultRepository resultRepository;

    @Mock
    private AcademicActivityRepository activityRepository;

    @Mock
    private StudentServiceClient studentServiceClient;

    @Mock
    private GradingService gradingService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ActivitySubmissionService submissionService;

    @InjectMocks
    private ActivityResultService resultService;

    private Long activityId = 10L;
    private Long teacherAId = 101L;
    private Long teacherBId = 102L;
    private Long studentAId = 201L;
    private Long studentBId = 202L;

    private AcademicActivity publishedActivity;

    @BeforeEach
    void setUp() {
        publishedActivity = AcademicActivity.builder()
                .id(activityId)
                .title("Class 8-A English Homework")
                .activityType(ActivityType.HOMEWORK)
                .studentClass("8")
                .section("A")
                .subject("English")
                .teacherId(teacherAId)
                .teacherName("Teacher A")
                .assignedDate(LocalDate.now().minusDays(1))
                .dueDate(LocalDate.now().plusDays(2))
                .maxMarks(BigDecimal.valueOf(10.0))
                .passingMarks(BigDecimal.valueOf(4.0))
                .status(ActivityStatus.PUBLISHED)
                .build();
    }

    // ── SUBMISSION TESTS ─────────────────────────────────────────

    @Test
    @DisplayName("Scenario 1: Student A submits work to activity for Student A's class -> SUCCESS")
    void testStudentASubmitsToOwnClassActivitySuccess() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(publishedActivity));
        when(studentServiceClient.getStudentById(studentAId)).thenReturn(
                StudentServiceClient.StudentInfoDto.builder()
                        .id(studentAId)
                        .firstName("Rahul")
                        .lastName("Sharma")
                        .admissionNo("ADM-201")
                        .studentClass("8")
                        .section("A")
                        .build()
        );

        when(submissionRepository.findByActivityIdAndStudentId(activityId, studentAId))
                .thenReturn(Optional.empty());

        when(submissionRepository.save(any(ActivitySubmission.class))).thenAnswer(inv -> {
            ActivitySubmission s = inv.getArgument(0);
            s.setId(1001L);
            return s;
        });

        SubmissionCreateRequest req = SubmissionCreateRequest.builder()
                .submissionText("Here is my completed English homework essay.")
                .build();

        SubmissionResponse resp = submissionService.submitWork(activityId, studentAId, req, 701L, "STUDENT");

        assertNotNull(resp);
        assertEquals(1001L, resp.getId());
        assertEquals(SubmissionStatus.SUBMITTED, resp.getStatus());
        assertEquals(1, resp.getAttemptNumber());
        verify(auditLogService).log(eq(701L), eq("STUDENT"), eq("SUBMISSION_CREATED"), anyString(), eq(1001L), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Scenario 2: Student A from another class/section attempts to submit -> 403 FORBIDDEN")
    void testStudentFromDifferentClassCannotSubmit() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(publishedActivity));
        when(studentServiceClient.getStudentById(studentAId)).thenReturn(
                StudentServiceClient.StudentInfoDto.builder()
                        .id(studentAId)
                        .firstName("Amit")
                        .lastName("Verma")
                        .studentClass("9") // Class mismatch: 9 vs 8
                        .section("A")
                        .build()
        );

        SubmissionCreateRequest req = SubmissionCreateRequest.builder()
                .submissionText("My submission")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                submissionService.submitWork(activityId, studentAId, req, 701L, "STUDENT")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Access denied"));
        verify(submissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Scenario 3: Late submission marked as LATE status when submitted after due date")
    void testLateSubmissionMarkedAsLate() {
        publishedActivity.setDueDate(LocalDate.now().minusDays(1)); // Due yesterday

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(publishedActivity));
        when(studentServiceClient.getStudentById(studentAId)).thenReturn(
                StudentServiceClient.StudentInfoDto.builder()
                        .id(studentAId)
                        .studentClass("8")
                        .section("A")
                        .build()
        );

        when(submissionRepository.findByActivityIdAndStudentId(activityId, studentAId))
                .thenReturn(Optional.empty());

        when(submissionRepository.save(any(ActivitySubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmissionCreateRequest req = SubmissionCreateRequest.builder()
                .submissionText("Late assignment response")
                .build();

        SubmissionResponse resp = submissionService.submitWork(activityId, studentAId, req, 701L, "STUDENT");

        assertNotNull(resp);
        assertEquals(SubmissionStatus.LATE, resp.getStatus());
    }

    @Test
    @DisplayName("Scenario 4: Student cannot submit if unauthenticated -> 401 UNAUTHORIZED")
    void testUnauthenticatedStudentCannotSubmit() {
        SubmissionCreateRequest req = SubmissionCreateRequest.builder()
                .submissionText("Anonymous text")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                submissionService.submitWork(activityId, null, req, null, "ANONYMOUS")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    // ── RESULT & MARKS TESTS ─────────────────────────────────────

    @Test
    @DisplayName("Scenario 5: Teacher A enters marks for own activity -> SUCCESS")
    void testTeacherAEntersMarksForOwnActivitySuccess() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(publishedActivity));
        when(resultRepository.findByActivityIdAndStudentId(activityId, studentAId)).thenReturn(Optional.empty());
        when(studentServiceClient.getStudentById(studentAId)).thenReturn(
                StudentServiceClient.StudentInfoDto.builder()
                        .id(studentAId)
                        .firstName("Rahul")
                        .lastName("Sharma")
                        .admissionNo("ADM-201")
                        .build()
        );

        when(resultRepository.save(any(ActivityResult.class))).thenAnswer(inv -> {
            ActivityResult r = inv.getArgument(0);
            r.setId(5001L);
            return r;
        });

        BatchActivityResultRequest req = BatchActivityResultRequest.builder()
                .entries(List.of(
                        ActivityResultEntryRequest.builder()
                                .studentId(studentAId)
                                .obtainedMarks(BigDecimal.valueOf(8.5))
                                .teacherFeedback("Great work!")
                                .build()
                ))
                .build();

        List<ActivityResultResponse> results = resultService.saveResultsBatch(
                activityId, req, teacherAId, 501L, "TEACHER", false
        );

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(BigDecimal.valueOf(8.5), results.get(0).getObtainedMarks());
        assertEquals(BigDecimal.valueOf(85.0).setScale(2), results.get(0).getPercentage());
        assertEquals(ResultStatus.DRAFT, results.get(0).getStatus());
    }

    @Test
    @DisplayName("Scenario 6: Teacher B attempts to enter marks for Teacher A's activity -> 403 FORBIDDEN")
    void testTeacherBCannotEnterMarksForTeacherAActivity() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(publishedActivity));

        BatchActivityResultRequest req = BatchActivityResultRequest.builder()
                .entries(List.of(
                        ActivityResultEntryRequest.builder()
                                .studentId(studentAId)
                                .obtainedMarks(BigDecimal.valueOf(9.0))
                                .build()
                ))
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                resultService.saveResultsBatch(activityId, req, teacherBId, 502L, "TEACHER", false)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You can only enter marks for your own activities"));
        verify(resultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Scenario 7: Teacher B attempts to publish Teacher A's results -> 403 FORBIDDEN")
    void testTeacherBCannotPublishTeacherAResults() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(publishedActivity));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                resultService.publishResults(activityId, teacherBId, 502L, "TEACHER", false)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(resultRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Scenario 8: Marks validation - obtained marks greater than max marks -> 400 BAD_REQUEST")
    void testMarksGreaterThanMaxMarksThrowsBadRequest() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(publishedActivity));

        BatchActivityResultRequest req = BatchActivityResultRequest.builder()
                .entries(List.of(
                        ActivityResultEntryRequest.builder()
                                .studentId(studentAId)
                                .obtainedMarks(BigDecimal.valueOf(15.0)) // Max marks is 10.0
                                .build()
                ))
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                resultService.saveResultsBatch(activityId, req, teacherAId, 501L, "TEACHER", false)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("cannot exceed maximum marks"));
    }

    @Test
    @DisplayName("Scenario 9: Marks validation - negative obtained marks -> 400 BAD_REQUEST")
    void testNegativeMarksThrowsBadRequest() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(publishedActivity));

        BatchActivityResultRequest req = BatchActivityResultRequest.builder()
                .entries(List.of(
                        ActivityResultEntryRequest.builder()
                                .studentId(studentAId)
                                .obtainedMarks(BigDecimal.valueOf(-2.0))
                                .build()
                ))
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                resultService.saveResultsBatch(activityId, req, teacherAId, 501L, "TEACHER", false)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("cannot be negative"));
    }

    @Test
    @DisplayName("Scenario 10: Student attempts to access unpublished result -> 403 FORBIDDEN")
    void testStudentCannotAccessUnpublishedResult() {
        ActivityResult draftResult = ActivityResult.builder()
                .id(1L)
                .activityId(activityId)
                .studentId(studentAId)
                .obtainedMarks(BigDecimal.valueOf(8.0))
                .maxMarks(BigDecimal.valueOf(10.0))
                .status(ResultStatus.DRAFT) // Not PUBLISHED yet
                .build();

        when(resultRepository.findByActivityIdAndStudentId(activityId, studentAId))
                .thenReturn(Optional.of(draftResult));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                resultService.getMyResult(activityId, studentAId)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("have not been published yet"));
    }

    @Test
    @DisplayName("Scenario 11: Student accesses published own result -> SUCCESS")
    void testStudentAccessesPublishedOwnResultSuccess() {
        ActivityResult pubResult = ActivityResult.builder()
                .id(1L)
                .activityId(activityId)
                .studentId(studentAId)
                .obtainedMarks(BigDecimal.valueOf(9.0))
                .maxMarks(BigDecimal.valueOf(10.0))
                .percentage(BigDecimal.valueOf(90.0))
                .status(ResultStatus.PUBLISHED)
                .teacherFeedback("Excellent essay writing")
                .build();

        when(resultRepository.findByActivityIdAndStudentId(activityId, studentAId))
                .thenReturn(Optional.of(pubResult));

        ActivityResultResponse resp = resultService.getMyResult(activityId, studentAId);

        assertNotNull(resp);
        assertEquals(BigDecimal.valueOf(9.0), resp.getObtainedMarks());
        assertEquals(ResultStatus.PUBLISHED, resp.getStatus());
        assertEquals("Excellent essay writing", resp.getTeacherFeedback());
    }

    @Test
    @DisplayName("Scenario 12: Admin manages and publishes activity results -> SUCCESS")
    void testAdminCanManageAndPublishResults() {
        when(activityRepository.findById(activityId)).thenReturn(Optional.of(publishedActivity));

        ActivityResult r1 = ActivityResult.builder()
                .id(1L)
                .activityId(activityId)
                .studentId(studentAId)
                .status(ResultStatus.DRAFT)
                .build();

        when(resultRepository.findByActivityId(activityId)).thenReturn(List.of(r1));
        when(resultRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<ActivityResultResponse> pub = resultService.publishResults(activityId, null, 1L, "ADMIN", true);

        assertEquals(1, pub.size());
        assertEquals(ResultStatus.PUBLISHED, pub.get(0).getStatus());
    }
}
