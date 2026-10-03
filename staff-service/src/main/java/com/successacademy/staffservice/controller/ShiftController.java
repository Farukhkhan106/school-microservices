package com.successacademy.staffservice.controller;

import com.successacademy.staffservice.dto.ShiftAssignmentRequest;
import com.successacademy.staffservice.dto.ShiftAssignmentResponse;
import com.successacademy.staffservice.dto.ShiftRequest;
import com.successacademy.staffservice.dto.ShiftResponse;
import com.successacademy.staffservice.security.SecurityContextUtil;
import com.successacademy.staffservice.service.ShiftService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/shifts")
@RequiredArgsConstructor
@Slf4j
public class ShiftController {

    private final ShiftService shiftService;
    private final SecurityContextUtil securityUtil;

    @GetMapping
    public ResponseEntity<List<ShiftResponse>> getAllShifts(
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        return ResponseEntity.ok(shiftService.getAllShifts(activeOnly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftResponse> getShiftById(@PathVariable Long id) {
        return ResponseEntity.ok(shiftService.getShiftById(id));
    }

    @PostMapping
    public ResponseEntity<ShiftResponse> createShift(
            @Valid @RequestBody ShiftRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        ShiftResponse response = shiftService.createShift(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShiftResponse> updateShift(
            @PathVariable Long id,
            @Valid @RequestBody ShiftRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        ShiftResponse response = shiftService.updateShift(id, request, actorUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assign")
    public ResponseEntity<ShiftAssignmentResponse> assignShift(
            @Valid @RequestBody ShiftAssignmentRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        ShiftAssignmentResponse response = shiftService.assignShiftToStaff(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<ShiftAssignmentResponse>> getShiftAssignmentsForStaff(
            @PathVariable Long staffId,
            HttpServletRequest req
    ) {
        securityUtil.requireSelfOrAdmin(req, staffId);
        List<ShiftAssignmentResponse> list = shiftService.getShiftAssignmentsForStaff(staffId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/staff/{staffId}/current")
    public ResponseEntity<ShiftAssignmentResponse> getCurrentShiftForStaff(
            @PathVariable Long staffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest req
    ) {
        securityUtil.requireSelfOrAdmin(req, staffId);
        if (date == null) {
            date = LocalDate.now();
        }
        ShiftAssignmentResponse response = shiftService.getCurrentShiftForStaff(staffId, date);
        return ResponseEntity.ok(response);
    }
}
