package com.successacademy.staffservice.controller;

import com.successacademy.staffservice.dto.LeaveApprovalRequest;
import com.successacademy.staffservice.dto.LeaveRequestDto;
import com.successacademy.staffservice.dto.LeaveResponseDto;
import com.successacademy.staffservice.security.SecurityContextUtil;
import com.successacademy.staffservice.service.LeaveService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
@Slf4j
public class LeaveController {

    private final LeaveService leaveService;
    private final SecurityContextUtil securityUtil;

    @PostMapping("/apply")
    public ResponseEntity<LeaveResponseDto> applyLeave(
            @Valid @RequestBody LeaveRequestDto request,
            HttpServletRequest req
    ) {
        Long staffId;
        if (securityUtil.isAdmin(req)) {
            staffId = request.getStaffId();
        } else {
            staffId = securityUtil.requireStaffIdentity(req);
        }
        Long actorUserId = securityUtil.getCurrentUserId(req);
        LeaveResponseDto response = leaveService.applyLeave(request, staffId, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<LeaveResponseDto>> getMyLeaves(HttpServletRequest req) {
        Long staffId = securityUtil.requireStaffIdentity(req);
        List<LeaveResponseDto> leaves = leaveService.getLeavesForStaff(staffId);
        return ResponseEntity.ok(leaves);
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<LeaveResponseDto>> getLeavesForStaff(
            @PathVariable Long staffId,
            HttpServletRequest req
    ) {
        securityUtil.requireSelfOrAdmin(req, staffId);
        List<LeaveResponseDto> leaves = leaveService.getLeavesForStaff(staffId);
        return ResponseEntity.ok(leaves);
    }

    @GetMapping
    public ResponseEntity<List<LeaveResponseDto>> getLeaves(
            @RequestParam(required = false) String status,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        List<LeaveResponseDto> leaves = leaveService.getLeavesByStatus(status);
        return ResponseEntity.ok(leaves);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<LeaveResponseDto> approveLeave(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        LeaveApprovalRequest request = LeaveApprovalRequest.builder()
                .action("APPROVE")
                .build();
        LeaveResponseDto response = leaveService.processLeaveApproval(id, request, actorUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<LeaveResponseDto> rejectLeave(
            @PathVariable Long id,
            @RequestBody(required = false) LeaveApprovalRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        LeaveApprovalRequest reqDto = (request != null) ? request : new LeaveApprovalRequest();
        reqDto.setAction("REJECT");
        LeaveResponseDto response = leaveService.processLeaveApproval(id, reqDto, actorUserId);
        return ResponseEntity.ok(response);
    }
}
