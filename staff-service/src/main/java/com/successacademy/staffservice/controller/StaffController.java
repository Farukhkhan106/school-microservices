package com.successacademy.staffservice.controller;

import com.successacademy.staffservice.dto.*;
import com.successacademy.staffservice.security.SecurityContextUtil;
import com.successacademy.staffservice.service.AuditLogService;
import com.successacademy.staffservice.service.StaffService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
@Slf4j
public class StaffController {

    private final StaffService staffService;
    private final AuditLogService auditLogService;
    private final SecurityContextUtil securityUtil;

    @PostMapping
    public ResponseEntity<StaffResponse> createStaff(
            @Valid @RequestBody StaffRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        StaffResponse response = staffService.createStaff(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffResponse> updateStaff(
            @PathVariable Long id,
            @Valid @RequestBody StaffRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        StaffResponse response = staffService.updateStaff(id, request, actorUserId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<StaffResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StaffStatusUpdateRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        StaffResponse response = staffService.updateStatus(id, request, actorUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/provision-access")
    public ResponseEntity<StaffResponse> provisionAccess(
            @PathVariable Long id,
            @Valid @RequestBody StaffProvisionRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        StaffResponse response = staffService.provisionAccess(id, request, actorUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/disable-access")
    public ResponseEntity<StaffResponse> disableAccess(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        StaffResponse response = staffService.disableAccess(id, actorUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<StaffResponse> getMyProfile(HttpServletRequest req) {
        Long staffId = securityUtil.requireStaffIdentity(req);
        StaffResponse response = staffService.getStaffById(staffId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<StaffStatsResponse> getStats(HttpServletRequest req) {
        securityUtil.requireAdmin(req);
        StaffStatsResponse stats = staffService.getStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/next-code")
    public ResponseEntity<Map<String, String>> getNextStaffCode(HttpServletRequest req) {
        securityUtil.requireAdmin(req);
        String code = staffService.getNextStaffCode();
        return ResponseEntity.ok(Map.of("staffCode", code));
    }

    @GetMapping
    public ResponseEntity<List<StaffResponse>> searchStaff(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String accessStatus,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        List<StaffResponse> list = staffService.searchStaff(
                keyword, category, departmentId, designationId, employmentType, status, accessStatus
        );
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffResponse> getStaffById(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireSelfOrAdmin(req, id);
        StaffResponse response = staffService.getStaffById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{staffCode}")
    public ResponseEntity<StaffResponse> getStaffByCode(
            @PathVariable String staffCode,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        StaffResponse response = staffService.getStaffByCode(staffCode);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest req
    ) {
        securityUtil.requireSelfOrAdmin(req, id);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String photoUrl = staffService.uploadPhoto(id, file, actorUserId);
        return ResponseEntity.ok(Map.of("photoUrl", photoUrl));
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<StaffAuditLogResponse>> getAuditLogs(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        List<StaffAuditLogResponse> logs = auditLogService.getLogsForStaff(id);
        return ResponseEntity.ok(logs);
    }
}
