package com.successacademy.facultyservice.controller;

import com.successacademy.facultyservice.dto.FacultyLeaveApprovalRequest;
import com.successacademy.facultyservice.dto.FacultyLeaveRequestDto;
import com.successacademy.facultyservice.dto.FacultyLeaveResponseDto;
import com.successacademy.facultyservice.security.SecurityContextUtil;
import com.successacademy.facultyservice.service.FacultyLeaveService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/faculty/leave")
@RequiredArgsConstructor
@Slf4j
public class FacultyLeaveController {

    private final FacultyLeaveService leaveService;
    private final SecurityContextUtil securityUtil;
    private final com.successacademy.facultyservice.repository.FacultyRepository facultyRepository;

    @GetMapping("/my")
    public ResponseEntity<List<FacultyLeaveResponseDto>> getMyLeaves(
            @RequestParam(required = false) Long teacherId,
            HttpServletRequest req
    ) {
        Long resolvedId = teacherId;
        Long userId = securityUtil.getCurrentUserId(req);
        String username = securityUtil.getCurrentUsername(req);

        if (resolvedId == null && userId != null) {
            com.successacademy.facultyservice.model.Faculty f = facultyRepository.findByUserId(userId).orElse(null);
            if (f != null) resolvedId = f.getId();
        }
        if (resolvedId == null && username != null) {
            com.successacademy.facultyservice.model.Faculty f = facultyRepository.findByEmail(username).orElse(null);
            if (f != null) resolvedId = f.getId();
        }
        if (resolvedId == null) {
            return ResponseEntity.ok(List.of());
        }

        List<FacultyLeaveResponseDto> res = leaveService.getLeavesForFaculty(resolvedId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/apply")
    public ResponseEntity<FacultyLeaveResponseDto> applyLeave(
            @RequestBody FacultyLeaveRequestDto request,
            HttpServletRequest req
    ) {
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String username = securityUtil.getCurrentUsername(req);

        if (!securityUtil.isAdmin(req)) {
            Long resolvedFacultyId = null;
            if (actorUserId != null) {
                com.successacademy.facultyservice.model.Faculty f = facultyRepository.findByUserId(actorUserId).orElse(null);
                if (f != null) resolvedFacultyId = f.getId();
            }
            if (resolvedFacultyId == null && username != null) {
                com.successacademy.facultyservice.model.Faculty f = facultyRepository.findByEmail(username).orElse(null);
                if (f != null) resolvedFacultyId = f.getId();
            }
            if (resolvedFacultyId == null && request.getFacultyId() != null) {
                resolvedFacultyId = request.getFacultyId();
            }
            if (resolvedFacultyId == null) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Could not resolve faculty identity for current user.");
            }
            request.setFacultyId(resolvedFacultyId);
        }

        FacultyLeaveResponseDto res = leaveService.applyLeave(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping
    public ResponseEntity<List<FacultyLeaveResponseDto>> getLeaves(
            @RequestParam(required = false) String status,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        List<FacultyLeaveResponseDto> res = leaveService.getLeavesByStatus(status);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/faculty/{facultyId}")
    public ResponseEntity<List<FacultyLeaveResponseDto>> getFacultyLeaves(
            @PathVariable Long facultyId,
            HttpServletRequest req
    ) {
        if (!securityUtil.isAdmin(req)) {
            Long userId = securityUtil.getCurrentUserId(req);
            String username = securityUtil.getCurrentUsername(req);
            boolean isSelf = false;
            if (userId != null) {
                com.successacademy.facultyservice.model.Faculty f = facultyRepository.findByUserId(userId).orElse(null);
                if (f != null && f.getId().equals(facultyId)) isSelf = true;
            }
            if (!isSelf && username != null) {
                com.successacademy.facultyservice.model.Faculty f = facultyRepository.findByEmail(username).orElse(null);
                if (f != null && f.getId().equals(facultyId)) isSelf = true;
            }
            if (!isSelf) {
                securityUtil.requireAdmin(req);
            }
        }
        List<FacultyLeaveResponseDto> res = leaveService.getLeavesForFaculty(facultyId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<FacultyLeaveResponseDto> approveLeave(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        FacultyLeaveApprovalRequest approvalReq = FacultyLeaveApprovalRequest.builder()
                .action("APPROVE")
                .build();
        FacultyLeaveResponseDto res = leaveService.processLeaveApproval(id, approvalReq, actorUserId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<FacultyLeaveResponseDto> rejectLeave(
            @PathVariable Long id,
            @RequestBody(required = false) FacultyLeaveApprovalRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        FacultyLeaveApprovalRequest rejectionReq = (request != null) ? request : new FacultyLeaveApprovalRequest();
        rejectionReq.setAction("REJECT");
        FacultyLeaveResponseDto res = leaveService.processLeaveApproval(id, rejectionReq, actorUserId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<FacultyLeaveResponseDto> cancelLeave(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        FacultyLeaveApprovalRequest cancelReq = FacultyLeaveApprovalRequest.builder()
                .action("CANCEL")
                .build();
        FacultyLeaveResponseDto res = leaveService.processLeaveApproval(id, cancelReq, actorUserId);
        return ResponseEntity.ok(res);
    }
}
