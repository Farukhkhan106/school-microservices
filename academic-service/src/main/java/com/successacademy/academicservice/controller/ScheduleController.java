package com.successacademy.academicservice.controller;

import com.successacademy.academicservice.dto.ScheduleCreateRequest;
import com.successacademy.academicservice.dto.ScheduleResponse;
import com.successacademy.academicservice.security.SecurityContextUtil;
import com.successacademy.academicservice.service.AssessmentScheduleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/academic/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final AssessmentScheduleService scheduleService;
    private final SecurityContextUtil securityUtil;

    @GetMapping("/assessment/{assessmentId}")
    public ResponseEntity<List<ScheduleResponse>> getByAssessment(@PathVariable Long assessmentId) {
        return ResponseEntity.ok(scheduleService.getSchedulesByAssessment(assessmentId));
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<ScheduleResponse>> getByTeacher(
            @PathVariable Long teacherId,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);
        return ResponseEntity.ok(scheduleService.getSchedulesByTeacher(teacherId));
    }

    @GetMapping("/my-schedules")
    public ResponseEntity<List<ScheduleResponse>> getMySchedules(HttpServletRequest req) {
        securityUtil.requireTeacher(req);
        Long currentTeacherId = securityUtil.getCurrentTeacherId(req);
        if (currentTeacherId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(scheduleService.getSchedulesByTeacher(currentTeacherId));
    }

    @GetMapping("/class")
    public ResponseEntity<List<ScheduleResponse>> getByClass(
            @RequestParam Long assessmentId,
            @RequestParam String studentClass,
            @RequestParam String section
    ) {
        return ResponseEntity.ok(scheduleService.getSchedulesByClassAndSection(assessmentId, studentClass, section));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getScheduleById(id));
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @Valid @RequestBody ScheduleCreateRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(request, actorUserId, role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        scheduleService.deleteSchedule(id, actorUserId, role);
        return ResponseEntity.noContent().build();
    }
}
