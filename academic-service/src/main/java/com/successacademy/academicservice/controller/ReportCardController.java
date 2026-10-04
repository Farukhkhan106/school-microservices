package com.successacademy.academicservice.controller;

import com.successacademy.academicservice.dto.StudentReportCardResponse;
import com.successacademy.academicservice.security.SecurityContextUtil;
import com.successacademy.academicservice.service.ReportCardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/academic/results")
@RequiredArgsConstructor
public class ReportCardController {

    private final ReportCardService reportCardService;
    private final SecurityContextUtil securityUtil;

    @GetMapping("/report-card")
    public ResponseEntity<StudentReportCardResponse> getReportCard(
            @RequestParam Long assessmentId,
            @RequestParam(required = false) Long studentId,
            HttpServletRequest req
    ) {
        Long targetStudentId = studentId;
        if (targetStudentId == null) {
            targetStudentId = securityUtil.getCurrentStudentId(req);
        }
        if (targetStudentId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required: Missing student identity.");
        }

        // Validate access
        securityUtil.validateStudentOrAdmin(req, targetStudentId);
        boolean isStudent = securityUtil.isStudent(req);

        return ResponseEntity.ok(reportCardService.getReportCard(assessmentId, targetStudentId, isStudent));
    }

    @GetMapping("/my-report-card")
    public ResponseEntity<StudentReportCardResponse> getMyReportCard(
            @RequestParam Long assessmentId,
            HttpServletRequest req
    ) {
        Long studentId = securityUtil.getCurrentStudentId(req);
        if (studentId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required: Missing student identity.");
        }
        return ResponseEntity.ok(reportCardService.getReportCard(assessmentId, studentId, true));
    }
}
