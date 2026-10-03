package com.successacademy.staffservice.controller;

import com.successacademy.staffservice.dto.DepartmentRequest;
import com.successacademy.staffservice.dto.DepartmentResponse;
import com.successacademy.staffservice.security.SecurityContextUtil;
import com.successacademy.staffservice.service.DepartmentService;
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
@RequestMapping("/departments")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    private final DepartmentService departmentService;
    private final SecurityContextUtil securityUtil;

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments(
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        return ResponseEntity.ok(departmentService.getAllDepartments(activeOnly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody DepartmentRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        DepartmentResponse response = departmentService.createDepartment(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        DepartmentResponse response = departmentService.updateDepartment(id, request, actorUserId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DepartmentResponse> updateDepartmentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String status = body.getOrDefault("status", "ACTIVE");
        DepartmentResponse response = departmentService.updateDepartmentStatus(id, status, actorUserId);
        return ResponseEntity.ok(response);
    }
}
