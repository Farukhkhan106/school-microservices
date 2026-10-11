package com.successacademy.academicservice.controller;

import com.successacademy.academicservice.dto.*;
import com.successacademy.academicservice.security.SecurityContextUtil;
import com.successacademy.academicservice.security.TenantContext;
import com.successacademy.academicservice.service.AcademicConfigurationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/academic")
@RequiredArgsConstructor
@Slf4j
public class AcademicConfigurationController {

    private final AcademicConfigurationService configService;
    private final SecurityContextUtil securityContextUtil;

    private void requireAdminOrTrusted(HttpServletRequest request) {
        if (!securityContextUtil.isTrustedInternalCall(request) && !securityContextUtil.isAdmin(request)) {
            // Also allow if running in development mode without gateway
            String role = securityContextUtil.getCurrentRole(request);
            if (!"ADMIN".equalsIgnoreCase(role) && !"SUPER_ADMIN".equalsIgnoreCase(role)) {
                // Check if Authorization header is present
                String auth = request.getHeader("Authorization");
                if (auth == null || auth.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin privileges required.");
                }
            }
        }
    }

    // ====================================================================
    // CLASSES
    // ====================================================================

    @GetMapping("/classes")
    public ResponseEntity<List<AcademicClassDto>> getClasses(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(configService.getClasses(tenantId, activeOnly));
    }

    @GetMapping("/classes/{id}")
    public ResponseEntity<AcademicClassDto> getClassById(@PathVariable Long id) {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(configService.getClassById(id, tenantId));
    }

    @PostMapping("/classes")
    public ResponseEntity<AcademicClassDto> createClass(
            @Valid @RequestBody CreateAcademicClassRequest req,
            HttpServletRequest request) {
        requireAdminOrTrusted(request);
        String tenantId = TenantContext.getTenantId();
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configService.createClass(req, tenantId, actorId, actorRole));
    }

    @PutMapping("/classes/{id}")
    public ResponseEntity<AcademicClassDto> updateClass(
            @PathVariable Long id,
            @Valid @RequestBody CreateAcademicClassRequest req,
            HttpServletRequest request) {
        requireAdminOrTrusted(request);
        String tenantId = TenantContext.getTenantId();
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);
        return ResponseEntity.ok(configService.updateClass(id, req, tenantId, actorId, actorRole));
    }

    @PatchMapping("/classes/{id}/status")
    public ResponseEntity<AcademicClassDto> updateClassStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        requireAdminOrTrusted(request);
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status field is required.");
        }
        String tenantId = TenantContext.getTenantId();
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);
        return ResponseEntity.ok(configService.updateClassStatus(id, status, tenantId, actorId, actorRole));
    }

    // ====================================================================
    // SECTIONS
    // ====================================================================

    @GetMapping("/sections")
    public ResponseEntity<List<AcademicSectionDto>> getSections(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(configService.getSections(tenantId, activeOnly));
    }

    @GetMapping("/sections/{id}")
    public ResponseEntity<AcademicSectionDto> getSectionById(@PathVariable Long id) {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(configService.getSectionById(id, tenantId));
    }

    @PostMapping("/sections")
    public ResponseEntity<AcademicSectionDto> createSection(
            @Valid @RequestBody CreateAcademicSectionRequest req,
            HttpServletRequest request) {
        requireAdminOrTrusted(request);
        String tenantId = TenantContext.getTenantId();
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configService.createSection(req, tenantId, actorId, actorRole));
    }

    @PutMapping("/sections/{id}")
    public ResponseEntity<AcademicSectionDto> updateSection(
            @PathVariable Long id,
            @Valid @RequestBody CreateAcademicSectionRequest req,
            HttpServletRequest request) {
        requireAdminOrTrusted(request);
        String tenantId = TenantContext.getTenantId();
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);
        return ResponseEntity.ok(configService.updateSection(id, req, tenantId, actorId, actorRole));
    }

    @PatchMapping("/sections/{id}/status")
    public ResponseEntity<AcademicSectionDto> updateSectionStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        requireAdminOrTrusted(request);
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status field is required.");
        }
        String tenantId = TenantContext.getTenantId();
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);
        return ResponseEntity.ok(configService.updateSectionStatus(id, status, tenantId, actorId, actorRole));
    }

    // ====================================================================
    // SUBJECTS
    // ====================================================================

    @GetMapping("/subjects")
    public ResponseEntity<List<AcademicSubjectDto>> getSubjects(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(configService.getSubjects(tenantId, activeOnly));
    }

    @GetMapping("/subjects/{id}")
    public ResponseEntity<AcademicSubjectDto> getSubjectById(@PathVariable Long id) {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(configService.getSubjectById(id, tenantId));
    }

    @PostMapping("/subjects")
    public ResponseEntity<AcademicSubjectDto> createSubject(
            @Valid @RequestBody CreateAcademicSubjectRequest req,
            HttpServletRequest request) {
        requireAdminOrTrusted(request);
        String tenantId = TenantContext.getTenantId();
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configService.createSubject(req, tenantId, actorId, actorRole));
    }

    @PutMapping("/subjects/{id}")
    public ResponseEntity<AcademicSubjectDto> updateSubject(
            @PathVariable Long id,
            @Valid @RequestBody CreateAcademicSubjectRequest req,
            HttpServletRequest request) {
        requireAdminOrTrusted(request);
        String tenantId = TenantContext.getTenantId();
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);
        return ResponseEntity.ok(configService.updateSubject(id, req, tenantId, actorId, actorRole));
    }

    @PatchMapping("/subjects/{id}/status")
    public ResponseEntity<AcademicSubjectDto> updateSubjectStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        requireAdminOrTrusted(request);
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status field is required.");
        }
        String tenantId = TenantContext.getTenantId();
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);
        return ResponseEntity.ok(configService.updateSubjectStatus(id, status, tenantId, actorId, actorRole));
    }

    // ====================================================================
    // STRUCTURE AGGREGATION
    // ====================================================================

    @GetMapping("/structure")
    public ResponseEntity<AcademicStructureResponse> getAcademicStructure() {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(configService.getAcademicStructure(tenantId));
    }

    // ====================================================================
    // SESSION OFFERINGS
    // ====================================================================

    @GetMapping("/sessions/{sessionId}/offerings/sections")
    public ResponseEntity<List<AcademicOfferingDto.ClassSectionOfferingDto>> getSessionClassSections(
            @PathVariable Long sessionId) {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(configService.getSessionClassSections(sessionId, tenantId));
    }

    @PostMapping("/sessions/{sessionId}/offerings/sections")
    public ResponseEntity<AcademicOfferingDto.ClassSectionOfferingDto> addSessionClassSection(
            @PathVariable Long sessionId,
            @RequestBody AcademicOfferingDto.CreateClassSectionOfferingRequest req,
            HttpServletRequest request) {
        requireAdminOrTrusted(request);
        String tenantId = TenantContext.getTenantId();
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configService.addSessionClassSection(sessionId, req, tenantId, actorId, actorRole));
    }

    @DeleteMapping("/sessions/{sessionId}/offerings/sections/{id}")
    public ResponseEntity<Map<String, String>> removeSessionClassSection(
            @PathVariable Long sessionId,
            @PathVariable Long id,
            HttpServletRequest request) {
        requireAdminOrTrusted(request);
        String tenantId = TenantContext.getTenantId();
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);
        configService.removeSessionClassSection(id, tenantId, actorId, actorRole);
        return ResponseEntity.ok(Map.of("message", "Offering removed successfully"));
    }

    @GetMapping("/sessions/{sessionId}/offerings/subjects")
    public ResponseEntity<List<AcademicOfferingDto.ClassSubjectOfferingDto>> getSessionClassSubjects(
            @PathVariable Long sessionId) {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(configService.getSessionClassSubjects(sessionId, tenantId));
    }

    @PostMapping("/sessions/{sessionId}/offerings/subjects")
    public ResponseEntity<AcademicOfferingDto.ClassSubjectOfferingDto> addSessionClassSubject(
            @PathVariable Long sessionId,
            @RequestBody AcademicOfferingDto.CreateClassSubjectOfferingRequest req,
            HttpServletRequest request) {
        requireAdminOrTrusted(request);
        String tenantId = TenantContext.getTenantId();
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(configService.addSessionClassSubject(sessionId, req, tenantId, actorId, actorRole));
    }

    @DeleteMapping("/sessions/{sessionId}/offerings/subjects/{id}")
    public ResponseEntity<Map<String, String>> removeSessionClassSubject(
            @PathVariable Long sessionId,
            @PathVariable Long id,
            HttpServletRequest request) {
        requireAdminOrTrusted(request);
        String tenantId = TenantContext.getTenantId();
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);
        configService.removeSessionClassSubject(id, tenantId, actorId, actorRole);
        return ResponseEntity.ok(Map.of("message", "Offering removed successfully"));
    }
}
