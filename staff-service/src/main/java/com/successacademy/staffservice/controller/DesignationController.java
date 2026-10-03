package com.successacademy.staffservice.controller;

import com.successacademy.staffservice.dto.DesignationRequest;
import com.successacademy.staffservice.dto.DesignationResponse;
import com.successacademy.staffservice.security.SecurityContextUtil;
import com.successacademy.staffservice.service.DesignationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/designations")
@RequiredArgsConstructor
@Slf4j
public class DesignationController {

    private final DesignationService designationService;
    private final SecurityContextUtil securityUtil;

    @GetMapping
    public ResponseEntity<List<DesignationResponse>> getAllDesignations(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        return ResponseEntity.ok(designationService.getAllDesignations(departmentId, activeOnly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DesignationResponse> getDesignationById(@PathVariable Long id) {
        return ResponseEntity.ok(designationService.getDesignationById(id));
    }

    @PostMapping
    public ResponseEntity<DesignationResponse> createDesignation(
            @Valid @RequestBody DesignationRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        DesignationResponse response = designationService.createDesignation(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DesignationResponse> updateDesignation(
            @PathVariable Long id,
            @Valid @RequestBody DesignationRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        DesignationResponse response = designationService.updateDesignation(id, request, actorUserId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DesignationResponse> updateDesignationStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String status = body.getOrDefault("status", "ACTIVE");
        DesignationResponse response = designationService.updateDesignationStatus(id, status, actorUserId);
        return ResponseEntity.ok(response);
    }
}
