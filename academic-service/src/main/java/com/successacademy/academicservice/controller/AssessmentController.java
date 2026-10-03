package com.successacademy.academicservice.controller;

import com.successacademy.academicservice.dto.AssessmentRequest;
import com.successacademy.academicservice.dto.AssessmentResponse;
import com.successacademy.academicservice.model.AssessmentStatus;
import com.successacademy.academicservice.security.SecurityContextUtil;
import com.successacademy.academicservice.service.AssessmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/academic/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final SecurityContextUtil securityUtil;

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<AssessmentResponse>> getBySession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(assessmentService.getAssessmentsBySession(sessionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssessmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.getAssessmentById(id));
    }

    @PostMapping
    public ResponseEntity<AssessmentResponse> createAssessment(
            @Valid @RequestBody AssessmentRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.createAssessment(request, actorUserId, role));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AssessmentResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam AssessmentStatus status,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        return ResponseEntity.ok(assessmentService.updateStatus(id, status, actorUserId, role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssessment(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        assessmentService.deleteAssessment(id, actorUserId, role);
        return ResponseEntity.noContent().build();
    }
}
