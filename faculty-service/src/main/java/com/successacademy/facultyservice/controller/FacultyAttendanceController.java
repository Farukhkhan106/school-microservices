package com.successacademy.facultyservice.controller;

import com.successacademy.facultyservice.dto.FacultyAttendanceRequest;
import com.successacademy.facultyservice.dto.FacultyAttendanceResponse;
import com.successacademy.facultyservice.dto.FacultyBulkAttendanceRequest;
import com.successacademy.facultyservice.security.SecurityContextUtil;
import com.successacademy.facultyservice.service.FacultyAttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/faculty/attendance")
@RequiredArgsConstructor
@Slf4j
public class FacultyAttendanceController {

    private final FacultyAttendanceService attendanceService;
    private final SecurityContextUtil securityUtil;

    @PostMapping("/mark")
    public ResponseEntity<FacultyAttendanceResponse> markAttendance(
            @RequestBody FacultyAttendanceRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        FacultyAttendanceResponse res = attendanceService.markAttendance(request, actorUserId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<FacultyAttendanceResponse>> bulkMarkAttendance(
            @RequestBody FacultyBulkAttendanceRequest bulkRequest,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        List<FacultyAttendanceResponse> res = attendanceService.bulkMarkAttendance(bulkRequest.getItems(), actorUserId);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/daily")
    public ResponseEntity<List<FacultyAttendanceResponse>> getDailyAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String department,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        List<FacultyAttendanceResponse> list = attendanceService.getAttendanceByDate(queryDate, department);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/month")
    public ResponseEntity<List<FacultyAttendanceResponse>> getMonthAttendance(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String department,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        LocalDate now = LocalDate.now();
        int m = (month != null) ? month : now.getMonthValue();
        int y = (year != null) ? year : now.getYear();
        List<FacultyAttendanceResponse> list = attendanceService.getAttendanceByMonth(m, y, department);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/faculty/{facultyId}")
    public ResponseEntity<List<FacultyAttendanceResponse>> getAttendanceForFaculty(
            @PathVariable Long facultyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        List<FacultyAttendanceResponse> list = attendanceService.getAttendanceForFaculty(facultyId, from, to);
        return ResponseEntity.ok(list);
    }
}
