package com.successacademy.academicservice.controller;

import com.successacademy.academicservice.dto.BatchMarksEntryRequest;
import com.successacademy.academicservice.dto.ScheduleMarksOverviewResponse;
import com.successacademy.academicservice.security.SecurityContextUtil;
import com.successacademy.academicservice.service.AssessmentScheduleService;
import com.successacademy.academicservice.service.MarksService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/academic/marks")
@RequiredArgsConstructor
public class MarksController {

    private final MarksService marksService;
    private final AssessmentScheduleService scheduleService;
    private final SecurityContextUtil securityUtil;

    @GetMapping("/schedule/{scheduleId}")
    public ResponseEntity<ScheduleMarksOverviewResponse> getMarksBySchedule(
            @PathVariable Long scheduleId,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);
        // If teacher, verify authorization for this schedule
        if (securityUtil.isTeacher(req)) {
            var schedule = scheduleService.getScheduleById(scheduleId);
            securityUtil.validateTeacherOrAdminForSchedule(req, schedule.getTeacherId());
        }
        return ResponseEntity.ok(marksService.getMarksForSchedule(scheduleId));
    }

    @PostMapping("/batch")
    public ResponseEntity<ScheduleMarksOverviewResponse> saveBatchMarks(
            @Valid @RequestBody BatchMarksEntryRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdminOrTeacher(req);
        var schedule = scheduleService.getScheduleById(request.getScheduleId());
        securityUtil.validateTeacherOrAdminForSchedule(req, schedule.getTeacherId());

        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        return ResponseEntity.ok(marksService.saveBatchMarks(request, actorUserId, role));
    }

    @PostMapping("/schedule/{scheduleId}/verify")
    public ResponseEntity<Void> verifyScheduleMarks(
            @PathVariable Long scheduleId,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        marksService.verifyScheduleMarks(scheduleId, actorUserId, role);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/schedule/{scheduleId}/lock")
    public ResponseEntity<Void> lockScheduleMarks(
            @PathVariable Long scheduleId,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        marksService.lockScheduleMarks(scheduleId, actorUserId, role);
        return ResponseEntity.ok().build();
    }
}
