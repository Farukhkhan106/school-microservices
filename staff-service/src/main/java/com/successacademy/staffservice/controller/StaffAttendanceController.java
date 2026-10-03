package com.successacademy.staffservice.controller;

import com.successacademy.staffservice.dto.SelfAttendanceRequest;
import com.successacademy.staffservice.dto.StaffAttendanceRequest;
import com.successacademy.staffservice.dto.StaffAttendanceResponse;
import com.successacademy.staffservice.security.SecurityContextUtil;
import com.successacademy.staffservice.service.StaffAttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@Slf4j
public class StaffAttendanceController {

    private final StaffAttendanceService attendanceService;
    private final SecurityContextUtil securityUtil;

    @PostMapping("/mark")
    public ResponseEntity<StaffAttendanceResponse> markAttendance(
            @Valid @RequestBody StaffAttendanceRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        StaffAttendanceResponse response = attendanceService.markAttendance(request, actorUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<StaffAttendanceResponse>> bulkMarkAttendance(
            @Valid @RequestBody List<StaffAttendanceRequest> requests,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        List<StaffAttendanceResponse> response = attendanceService.bulkMarkAttendance(requests, actorUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/daily")
    public ResponseEntity<List<StaffAttendanceResponse>> getDailyAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long departmentId,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        if (date == null) {
            date = LocalDate.now();
        }
        List<StaffAttendanceResponse> response = attendanceService.getAttendanceByDate(date, departmentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/month")
    public ResponseEntity<List<StaffAttendanceResponse>> getMonthAttendance(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long departmentId,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        LocalDate now = LocalDate.now();
        int m = (month != null) ? month : now.getMonthValue();
        int y = (year != null) ? year : now.getYear();
        List<StaffAttendanceResponse> response = attendanceService.getAttendanceByMonth(m, y, departmentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<StaffAttendanceResponse>> getAttendanceForStaff(
            @PathVariable Long staffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest req
    ) {
        securityUtil.requireSelfOrAdmin(req, staffId);
        if (from == null) {
            from = LocalDate.now().minusDays(30);
        }
        if (to == null) {
            to = LocalDate.now();
        }
        List<StaffAttendanceResponse> response = attendanceService.getAttendanceForStaff(staffId, from, to);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/check-in")
    public ResponseEntity<StaffAttendanceResponse> selfCheckIn(
            @RequestBody(required = false) SelfAttendanceRequest request,
            HttpServletRequest req
    ) {
        Long staffId = securityUtil.requireStaffIdentity(req);
        String remarks = (request != null) ? request.getRemarks() : null;
        StaffAttendanceResponse response = attendanceService.selfCheckIn(staffId, remarks);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/check-out")
    public ResponseEntity<StaffAttendanceResponse> selfCheckOut(
            @RequestBody(required = false) SelfAttendanceRequest request,
            HttpServletRequest req
    ) {
        Long staffId = securityUtil.requireStaffIdentity(req);
        String remarks = (request != null) ? request.getRemarks() : null;
        StaffAttendanceResponse response = attendanceService.selfCheckOut(staffId, remarks);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<StaffAttendanceResponse> getMyTodayAttendance(HttpServletRequest req) {
        Long staffId = securityUtil.requireStaffIdentity(req);
        StaffAttendanceResponse response = attendanceService.getTodayAttendance(staffId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/history")
    public ResponseEntity<List<StaffAttendanceResponse>> getMyAttendanceHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest req
    ) {
        Long staffId = securityUtil.requireStaffIdentity(req);
        if (from == null) {
            from = LocalDate.now().minusDays(30);
        }
        if (to == null) {
            to = LocalDate.now();
        }
        List<StaffAttendanceResponse> response = attendanceService.getAttendanceForStaff(staffId, from, to);
        return ResponseEntity.ok(response);
    }
}
