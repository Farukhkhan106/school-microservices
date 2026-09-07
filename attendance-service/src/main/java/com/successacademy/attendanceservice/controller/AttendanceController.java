package com.successacademy.attendanceservice.controller;

import com.successacademy.attendanceservice.dto.AttendanceRequest;
import com.successacademy.attendanceservice.dto.AttendanceSummaryResponse;
import com.successacademy.attendanceservice.dto.BulkAttendanceRequest;
import com.successacademy.attendanceservice.model.Attendance;
import com.successacademy.attendanceservice.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;

    // ── TEACHER ─────────────────────────────────────────────────

    // Mark single student attendance
    @PostMapping("/mark")
    public ResponseEntity<Attendance> mark(@RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(service.markAttendance(request));
    }

    // Bulk save — whole class at once (Save Attendance button)
    @PostMapping("/mark/bulk")
    public ResponseEntity<List<Attendance>> markBulk(
            @RequestBody BulkAttendanceRequest request) {
        return ResponseEntity.ok(service.markBulkAttendance(request));
    }

    // Fetch existing attendance for a class+section+date (load for editing)
    @GetMapping("/class")
    public ResponseEntity<List<Attendance>> getClassAttendance(
            @RequestParam String studentClass,
            @RequestParam(required = false) String section,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(
                service.getClassAttendance(studentClass, section, date));
    }

    // ── STUDENT ─────────────────────────────────────────────────

    // Full attendance history for a student
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Attendance>> getStudentAttendance(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(service.getStudentAttendance(studentId));
    }

    // Attendance between dates
    @GetMapping("/student/{studentId}/range")
    public ResponseEntity<List<Attendance>> getStudentRange(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                service.getStudentAttendanceBetween(studentId, from, to));
    }

    // Attendance summary — overall % + per-subject % (StudentAcademics page)
    @GetMapping("/student/{studentId}/summary")
    public ResponseEntity<AttendanceSummaryResponse> getSummary(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(service.getStudentSummary(studentId));
    }

    // ── ADMIN ────────────────────────────────────────────────────

    // All attendance records
    @GetMapping("/all")
    public ResponseEntity<List<Attendance>> all() {
        return ResponseEntity.ok(service.getAllAttendance());
    }
}
