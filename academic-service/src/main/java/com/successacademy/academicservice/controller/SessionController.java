package com.successacademy.academicservice.controller;

import com.successacademy.academicservice.dto.*;
import com.successacademy.academicservice.security.SecurityContextUtil;
import com.successacademy.academicservice.service.AcademicSessionService;
import com.successacademy.academicservice.service.AcademicSessionTransitionService;
import com.successacademy.academicservice.service.SessionBootstrapService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/academic/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final AcademicSessionService sessionService;
    private final AcademicSessionTransitionService transitionService;
    private final SessionBootstrapService bootstrapService;
    private final SecurityContextUtil securityUtil;

    // ── SESSION LIFECYCLE ────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getAllSessions(HttpServletRequest req) {
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(sessionService.getAllSessions(tenantId));
    }

    @GetMapping("/active")
    public ResponseEntity<SessionResponse> getActiveSession(HttpServletRequest req) {
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(sessionService.getActiveSession(tenantId));
    }

    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable Long id, HttpServletRequest req) {
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(sessionService.getSessionById(id, tenantId));
    }

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
            @Valid @RequestBody SessionRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionService.createSession(request, tenantId, actorUserId, role));
    }

    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<SessionResponse> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody SessionRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(sessionService.updateSession(id, request, tenantId, actorUserId, role));
    }

    @PatchMapping("/{id:[0-9]+}/activate")
    public ResponseEntity<SessionResponse> activateSession(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(sessionService.activateSession(id, tenantId, actorUserId, role));
    }

    @PatchMapping("/{id:[0-9]+}/close")
    public ResponseEntity<SessionResponse> closeSession(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(sessionService.closeSession(id, tenantId, actorUserId, role));
    }

    @PatchMapping("/{id:[0-9]+}/archive")
    public ResponseEntity<SessionResponse> archiveSession(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(sessionService.archiveSession(id, tenantId, actorUserId, role));
    }

    // ── ENROLLMENTS ──────────────────────────────────────────────

    @GetMapping("/{id:[0-9]+}/enrollments")
    public ResponseEntity<List<StudentEnrollmentResponse>> getEnrollments(
            @PathVariable Long id,
            @RequestParam(required = false) String studentClass,
            @RequestParam(required = false) String section,
            HttpServletRequest req
    ) {
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(sessionService.getEnrollmentsBySession(id, studentClass, section, tenantId));
    }

    @GetMapping("/student/{studentId:[0-9]+}/history")
    public ResponseEntity<List<StudentEnrollmentResponse>> getStudentHistory(
            @PathVariable Long studentId,
            HttpServletRequest req
    ) {
        securityUtil.validateStudentOrAdmin(req, studentId);
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(sessionService.getStudentEnrollmentHistory(studentId, tenantId));
    }

    @PostMapping("/enroll")
    public ResponseEntity<StudentEnrollmentResponse> enrollStudent(
            @Valid @RequestBody StudentEnrollmentRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.enrollStudent(request, tenantId));
    }

    // ── YEAR TRANSITION WORKFLOW ─────────────────────────────────

    @PostMapping("/transitions/preview")
    public ResponseEntity<TransitionPreviewResponse> previewTransition(
            @Valid @RequestBody SessionTransitionRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(transitionService.previewTransition(request, tenantId, actorUserId));
    }

    @GetMapping("/transitions/{id:[0-9]+}")
    public ResponseEntity<TransitionPreviewResponse> getTransition(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(transitionService.getTransitionById(id, tenantId));
    }

    @PutMapping("/transitions/{id:[0-9]+}/promotions/{studentId:[0-9]+}")
    public ResponseEntity<StudentPromotionItemDto> updatePromotionDecision(
            @PathVariable Long id,
            @PathVariable Long studentId,
            @Valid @RequestBody PromotionDecisionUpdateRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(transitionService.updatePromotionDecision(id, studentId, request, tenantId, actorUserId));
    }

    @PostMapping("/transitions/{id:[0-9]+}/execute")
    public ResponseEntity<TransitionPreviewResponse> executeTransition(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(transitionService.executeTransition(id, tenantId, actorUserId, role));
    }

    // ── DATA BOOTSTRAP / FULL-ERP SYNCHRONIZATION ────────────────
    @PostMapping("/bootstrap/preview")
    public ResponseEntity<SessionBootstrapPreviewResponse> previewBootstrap(HttpServletRequest req) {
        securityUtil.requireAdmin(req);
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(bootstrapService.previewBootstrap(tenantId));
    }

    @PostMapping("/bootstrap/execute")
    public ResponseEntity<SessionBootstrapExecuteResponse> executeBootstrap(HttpServletRequest req) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        String tenantId = securityUtil.getTenantId(req);
        return ResponseEntity.ok(bootstrapService.executeBootstrap(tenantId, actorUserId, role));
    }
}
