package com.successacademy.academicservice.controller;

import com.successacademy.academicservice.dto.*;
import com.successacademy.academicservice.model.ActivityStatus;
import com.successacademy.academicservice.model.ActivityType;
import com.successacademy.academicservice.security.SecurityContextUtil;
import com.successacademy.academicservice.service.AcademicActivityService;
import com.successacademy.academicservice.service.ActivityResultService;
import com.successacademy.academicservice.service.ActivitySubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/academic/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final AcademicActivityService activityService;
    private final ActivitySubmissionService submissionService;
    private final ActivityResultService resultService;
    private final SecurityContextUtil securityUtil;

    @PostMapping
    public ResponseEntity<ActivityResponse> createActivity(
            @Valid @RequestBody ActivityCreateRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);

        Long teacherId = securityUtil.getCurrentTeacherId(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        String username = securityUtil.getCurrentUsername(req);
        boolean isAdmin = securityUtil.isAdmin(req);

        ActivityResponse created = activityService.createActivity(
                request,
                teacherId,
                username,
                actorUserId,
                role,
                isAdmin
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ActivityResponse>> getMyTeacherActivities(
            @RequestParam(required = false) String studentClass,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) ActivityType type,
            @RequestParam(required = false) ActivityStatus status,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);
        Long teacherId = securityUtil.getCurrentTeacherId(req);
        return ResponseEntity.ok(activityService.getTeacherActivities(
                teacherId, studentClass, section, subject, type, status
        ));
    }

    @GetMapping("/student/my")
    public ResponseEntity<List<ActivityResponse>> getMyStudentActivities(
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) ActivityType type,
            HttpServletRequest req
    ) {
        securityUtil.requireStudentOrAdmin(req);
        Long studentId = securityUtil.getCurrentStudentId(req);
        return ResponseEntity.ok(activityService.getStudentActivities(
                studentId, subject, type
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityResponse> getActivityById(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        Long teacherId = securityUtil.getCurrentTeacherId(req);
        Long studentId = securityUtil.getCurrentStudentId(req);

        return ResponseEntity.ok(activityService.getActivityById(id, actorUserId, role, teacherId, studentId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable Long id,
            @Valid @RequestBody ActivityUpdateRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);

        Long teacherId = securityUtil.getCurrentTeacherId(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        boolean isAdmin = securityUtil.isAdmin(req);

        return ResponseEntity.ok(activityService.updateActivity(
                id, request, teacherId, actorUserId, role, isAdmin
        ));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<ActivityResponse> publishActivity(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);

        Long teacherId = securityUtil.getCurrentTeacherId(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        boolean isAdmin = securityUtil.isAdmin(req);

        return ResponseEntity.ok(activityService.updateStatus(
                id, ActivityStatus.PUBLISHED, teacherId, actorUserId, role, isAdmin
        ));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<ActivityResponse> closeActivity(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);

        Long teacherId = securityUtil.getCurrentTeacherId(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        boolean isAdmin = securityUtil.isAdmin(req);

        return ResponseEntity.ok(activityService.updateStatus(
                id, ActivityStatus.CLOSED, teacherId, actorUserId, role, isAdmin
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);

        Long teacherId = securityUtil.getCurrentTeacherId(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        boolean isAdmin = securityUtil.isAdmin(req);

        activityService.deleteActivity(id, teacherId, actorUserId, role, isAdmin);
        return ResponseEntity.noContent().build();
    }

    // ── SUBMISSIONS (Student Participation) ──────────────────────

    @PostMapping("/{activityId}/submissions")
    public ResponseEntity<SubmissionResponse> submitWork(
            @PathVariable Long activityId,
            @Valid @RequestBody SubmissionCreateRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireStudentOrAdmin(req);
        Long studentId = securityUtil.getCurrentStudentId(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(submissionService.submitWork(activityId, studentId, request, actorUserId, role));
    }

    @GetMapping("/{activityId}/submissions/my")
    public ResponseEntity<SubmissionResponse> getMySubmission(
            @PathVariable Long activityId,
            HttpServletRequest req
    ) {
        securityUtil.requireStudentOrAdmin(req);
        Long studentId = securityUtil.getCurrentStudentId(req);
        SubmissionResponse resp = submissionService.getMySubmission(activityId, studentId);
        if (resp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{activityId}/submissions")
    public ResponseEntity<List<SubmissionResponse>> getSubmissionsByActivity(
            @PathVariable Long activityId,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);
        Long teacherId = securityUtil.getCurrentTeacherId(req);
        boolean isAdmin = securityUtil.isAdmin(req);
        return ResponseEntity.ok(submissionService.getSubmissionsByActivity(activityId, teacherId, isAdmin));
    }

    // ── ROSTER & RESULTS (Teacher Evaluation & Marks) ───────────

    @GetMapping("/{activityId}/roster")
    public ResponseEntity<List<ActivityRosterStudentResponse>> getActivityRoster(
            @PathVariable Long activityId,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);
        Long teacherId = securityUtil.getCurrentTeacherId(req);
        boolean isAdmin = securityUtil.isAdmin(req);
        return ResponseEntity.ok(resultService.getActivityRoster(activityId, teacherId, isAdmin));
    }

    @PostMapping("/{activityId}/results/batch")
    public ResponseEntity<List<ActivityResultResponse>> saveResultsBatch(
            @PathVariable Long activityId,
            @Valid @RequestBody BatchActivityResultRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);
        Long teacherId = securityUtil.getCurrentTeacherId(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        boolean isAdmin = securityUtil.isAdmin(req);

        return ResponseEntity.ok(resultService.saveResultsBatch(
                activityId, request, teacherId, actorUserId, role, isAdmin
        ));
    }

    @PatchMapping("/{activityId}/results/finalize")
    public ResponseEntity<List<ActivityResultResponse>> finalizeResults(
            @PathVariable Long activityId,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);
        Long teacherId = securityUtil.getCurrentTeacherId(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        boolean isAdmin = securityUtil.isAdmin(req);

        return ResponseEntity.ok(resultService.finalizeResults(
                activityId, teacherId, actorUserId, role, isAdmin
        ));
    }

    @PatchMapping("/{activityId}/results/publish")
    public ResponseEntity<List<ActivityResultResponse>> publishResults(
            @PathVariable Long activityId,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);
        Long teacherId = securityUtil.getCurrentTeacherId(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        boolean isAdmin = securityUtil.isAdmin(req);

        return ResponseEntity.ok(resultService.publishResults(
                activityId, teacherId, actorUserId, role, isAdmin
        ));
    }

    @GetMapping("/{activityId}/results/my")
    public ResponseEntity<ActivityResultResponse> getMyResult(
            @PathVariable Long activityId,
            HttpServletRequest req
    ) {
        securityUtil.requireStudentOrAdmin(req);
        Long studentId = securityUtil.getCurrentStudentId(req);
        return ResponseEntity.ok(resultService.getMyResult(activityId, studentId));
    }

    @GetMapping("/results/student/my")
    public ResponseEntity<List<ActivityResultResponse>> getMyStudentResults(
            HttpServletRequest req
    ) {
        securityUtil.requireStudentOrAdmin(req);
        Long studentId = securityUtil.getCurrentStudentId(req);
        return ResponseEntity.ok(resultService.getStudentPublishedResults(studentId));
    }
}
