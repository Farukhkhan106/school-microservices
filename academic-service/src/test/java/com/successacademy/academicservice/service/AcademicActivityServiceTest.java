package com.successacademy.academicservice.service;

import com.successacademy.academicservice.client.FacultyServiceClient;
import com.successacademy.academicservice.client.StudentServiceClient;
import com.successacademy.academicservice.dto.ActivityCreateRequest;
import com.successacademy.academicservice.dto.ActivityResponse;
import com.successacademy.academicservice.dto.ActivityUpdateRequest;
import com.successacademy.academicservice.model.AcademicActivity;
import com.successacademy.academicservice.model.ActivityStatus;
import com.successacademy.academicservice.model.ActivityType;
import com.successacademy.academicservice.repository.AcademicActivityRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicActivityServiceTest {

    @Mock
    private AcademicActivityRepository activityRepository;

    @Mock
    private FacultyServiceClient facultyServiceClient;

    @Mock
    private StudentServiceClient studentServiceClient;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AcademicActivityService activityService;

    private Long teacherAId = 101L;
    private Long teacherBId = 102L;
    private Long studentAId = 201L;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Scenario 1: Teacher A assigned to 8-A English creates 8-A English Homework -> SUCCESS")
    void testTeacherACreatesAssignedActivitySuccess() {
        ActivityCreateRequest req = ActivityCreateRequest.builder()
                .title("English Grammar Homework")
                .activityType(ActivityType.HOMEWORK)
                .studentClass("8")
                .section("A")
                .subject("English")
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(2))
                .maxMarks(BigDecimal.valueOf(10.0))
                .passingMarks(BigDecimal.valueOf(4.0))
                .build();

        when(facultyServiceClient.isTeacherAssigned(teacherAId, "8", "A", "English")).thenReturn(true);
        when(activityRepository.save(any(AcademicActivity.class))).thenAnswer(inv -> {
            AcademicActivity act = inv.getArgument(0);
            act.setId(1L);
            return act;
        });

        ActivityResponse resp = activityService.createActivity(req, teacherAId, "Teacher A", 501L, "TEACHER", false);

        assertNotNull(resp);
        assertEquals(1L, resp.getId());
        assertEquals("English Grammar Homework", resp.getTitle());
        assertEquals(ActivityStatus.DRAFT, resp.getStatus());
        verify(auditLogService, times(1)).log(eq(501L), eq("TEACHER"), eq("ACTIVITY_CREATED"), anyString(), eq(1L), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Scenario 2: Teacher A assigned to 8-A English attempts to create 8-A Math activity -> 403 FORBIDDEN")
    void testTeacherACreatesUnassignedSubjectThrowsForbidden() {
        ActivityCreateRequest req = ActivityCreateRequest.builder()
                .title("Math Algebra Test")
                .activityType(ActivityType.CLASS_TEST)
                .studentClass("8")
                .section("A")
                .subject("Mathematics")
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(1))
                .maxMarks(BigDecimal.valueOf(20.0))
                .build();

        when(facultyServiceClient.isTeacherAssigned(teacherAId, "8", "A", "Mathematics")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                activityService.createActivity(req, teacherAId, "Teacher A", 501L, "TEACHER", false)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You are not assigned to teach Mathematics"));
        verify(activityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Scenario 3: Teacher A assigned to 8-A English attempts to create 9-A English activity -> 403 FORBIDDEN")
    void testTeacherACreatesUnassignedClassThrowsForbidden() {
        ActivityCreateRequest req = ActivityCreateRequest.builder()
                .title("Class 9 English Essay")
                .activityType(ActivityType.ASSIGNMENT)
                .studentClass("9")
                .section("A")
                .subject("English")
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(3))
                .maxMarks(BigDecimal.valueOf(15.0))
                .build();

        when(facultyServiceClient.isTeacherAssigned(teacherAId, "9", "A", "English")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                activityService.createActivity(req, teacherAId, "Teacher A", 501L, "TEACHER", false)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You are not assigned to teach English for Class 9-A"));
        verify(activityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Scenario 4: Teacher B attempts to update Teacher A's activity -> 403 FORBIDDEN")
    void testTeacherBUpdatesTeacherAActivityThrowsForbidden() {
        AcademicActivity activity = AcademicActivity.builder()
                .id(10L)
                .title("Teacher A's Test")
                .teacherId(teacherAId)
                .studentClass("8")
                .section("A")
                .subject("English")
                .maxMarks(BigDecimal.valueOf(10.0))
                .status(ActivityStatus.DRAFT)
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(2))
                .build();

        when(activityRepository.findById(10L)).thenReturn(Optional.of(activity));

        ActivityUpdateRequest updateReq = ActivityUpdateRequest.builder()
                .title("Hacked Title")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                activityService.updateActivity(10L, updateReq, teacherBId, 502L, "TEACHER", false)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You can only edit your own activities"));
        verify(activityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Scenario 5: Student A views published activity assigned to Student A's class -> SUCCESS")
    void testStudentAViewsOwnClassActivitySuccess() {
        AcademicActivity activity = AcademicActivity.builder()
                .id(15L)
                .title("Class 8-A English Assignment")
                .studentClass("8")
                .section("A")
                .subject("English")
                .teacherId(teacherAId)
                .status(ActivityStatus.PUBLISHED)
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(2))
                .maxMarks(BigDecimal.valueOf(10.0))
                .build();

        when(activityRepository.findById(15L)).thenReturn(Optional.of(activity));
        when(studentServiceClient.getStudentById(studentAId)).thenReturn(
                StudentServiceClient.StudentInfoDto.builder()
                        .id(studentAId)
                        .studentClass("8")
                        .section("A")
                        .build()
        );

        ActivityResponse resp = activityService.getActivityById(15L, 701L, "STUDENT", null, studentAId);

        assertNotNull(resp);
        assertEquals(15L, resp.getId());
        assertEquals("Class 8-A English Assignment", resp.getTitle());
    }

    @Test
    @DisplayName("Scenario 6: Student A attempts to view activity for another class -> 403 FORBIDDEN")
    void testStudentAViewsOtherClassActivityThrowsForbidden() {
        AcademicActivity activity = AcademicActivity.builder()
                .id(20L)
                .title("Class 9-A Science Activity")
                .studentClass("9")
                .section("A")
                .subject("Science")
                .teacherId(teacherAId)
                .status(ActivityStatus.PUBLISHED)
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(2))
                .maxMarks(BigDecimal.valueOf(10.0))
                .build();

        when(activityRepository.findById(20L)).thenReturn(Optional.of(activity));
        when(studentServiceClient.getStudentById(studentAId)).thenReturn(
                StudentServiceClient.StudentInfoDto.builder()
                        .id(studentAId)
                        .studentClass("8")
                        .section("A")
                        .build()
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                activityService.getActivityById(20L, 701L, "STUDENT", null, studentAId)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Activity does not belong to your class"));
    }

    @Test
    @DisplayName("Scenario 7: Date validation - dueDate before assignedDate -> 400 BAD_REQUEST")
    void testDueDateBeforeAssignedDateThrowsBadRequest() {
        ActivityCreateRequest req = ActivityCreateRequest.builder()
                .title("Invalid Date Test")
                .activityType(ActivityType.HOMEWORK)
                .studentClass("8")
                .section("A")
                .subject("English")
                .assignedDate(LocalDate.now().plusDays(5))
                .dueDate(LocalDate.now()) // Invalid: earlier than assigned date
                .maxMarks(BigDecimal.valueOf(10.0))
                .build();

        when(facultyServiceClient.isTeacherAssigned(teacherAId, "8", "A", "English")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                activityService.createActivity(req, teacherAId, "Teacher A", 501L, "TEACHER", false)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Due date cannot be earlier than assigned date"));
    }

    @Test
    @DisplayName("Scenario 8: Marks validation - passing marks greater than max marks -> 400 BAD_REQUEST")
    void testPassingMarksGreaterThanMaxMarksThrowsBadRequest() {
        ActivityCreateRequest req = ActivityCreateRequest.builder()
                .title("Invalid Marks Test")
                .activityType(ActivityType.UNIT_TEST)
                .studentClass("8")
                .section("A")
                .subject("English")
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(2))
                .maxMarks(BigDecimal.valueOf(10.0))
                .passingMarks(BigDecimal.valueOf(15.0)) // Invalid: 15 > 10
                .build();

        when(facultyServiceClient.isTeacherAssigned(teacherAId, "8", "A", "English")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                activityService.createActivity(req, teacherAId, "Teacher A", 501L, "TEACHER", false)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Passing marks cannot exceed max marks"));
    }

    @Test
    @DisplayName("Scenario 9: Admin creates activity bypassing teacher assignment check -> SUCCESS")
    void testAdminCreatesActivityBypassingAssignmentCheck() {
        ActivityCreateRequest req = ActivityCreateRequest.builder()
                .title("Principal Special Essay")
                .activityType(ActivityType.PROJECT)
                .studentClass("12")
                .section("A")
                .subject("Economics")
                .assignedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(10))
                .maxMarks(BigDecimal.valueOf(50.0))
                .build();

        when(activityRepository.save(any(AcademicActivity.class))).thenAnswer(inv -> {
            AcademicActivity act = inv.getArgument(0);
            act.setId(99L);
            return act;
        });

        ActivityResponse resp = activityService.createActivity(req, null, "Admin User", 1L, "ADMIN", true);

        assertNotNull(resp);
        assertEquals(99L, resp.getId());
        verify(facultyServiceClient, never()).isTeacherAssigned(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Scenario 10: Student only receives PUBLISHED activities in listing")
    void testStudentOnlyReceivesPublishedActivities() {
        when(studentServiceClient.getStudentById(studentAId)).thenReturn(
                StudentServiceClient.StudentInfoDto.builder()
                        .id(studentAId)
                        .studentClass("8")
                        .section("A")
                        .build()
        );

        AcademicActivity pubAct = AcademicActivity.builder()
                .id(1L)
                .title("Published Homework")
                .status(ActivityStatus.PUBLISHED)
                .studentClass("8")
                .section("A")
                .subject("English")
                .build();

        when(activityRepository.findStudentActivitiesFiltered("8", "A", ActivityStatus.PUBLISHED, null, null))
                .thenReturn(List.of(pubAct));

        List<ActivityResponse> activities = activityService.getStudentActivities(studentAId, null, null);

        assertEquals(1, activities.size());
        assertEquals("Published Homework", activities.get(0).getTitle());
        verify(activityRepository).findStudentActivitiesFiltered("8", "A", ActivityStatus.PUBLISHED, null, null);
    }
}
