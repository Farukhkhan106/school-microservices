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

import com.successacademy.attendanceservice.client.FacultyServiceClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;
    private final FacultyServiceClient facultyServiceClient;

    // ── TEACHER ─────────────────────────────────────────────────

    // Mark single student attendance
    @PostMapping("/mark")
    public ResponseEntity<Attendance> mark(
            @RequestBody AttendanceRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if ("TEACHER".equalsIgnoreCase(role)) {
            if (!facultyServiceClient.hasClassTeacherAccess(userId, request.getStudentClass(), request.getSection())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only the Class Teacher of " + request.getStudentClass() + "-" + request.getSection() + " can submit class attendance.");
            }
        }
        return ResponseEntity.ok(service.markAttendance(request));
    }

    // Bulk save — whole class at once (Save Attendance button)
    @PostMapping("/mark/bulk")
    public ResponseEntity<List<Attendance>> markBulk(
            @RequestBody BulkAttendanceRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if ("TEACHER".equalsIgnoreCase(role) && request.getRecords() != null && !request.getRecords().isEmpty()) {
            AttendanceRequest first = request.getRecords().get(0);
            if (!facultyServiceClient.hasClassTeacherAccess(userId, first.getStudentClass(), first.getSection())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only the Class Teacher of " + first.getStudentClass() + "-" + first.getSection() + " can submit class attendance.");
            }
        }
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
            @PathVariable Long studentId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-Student-Id", required = false) Long currentStudentId) {
        enforceStudentAccess(studentId, role, currentStudentId);
        return ResponseEntity.ok(service.getStudentAttendance(studentId));
    }

    // Attendance between dates
    @GetMapping("/student/{studentId}/range")
    public ResponseEntity<List<Attendance>> getStudentRange(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-Student-Id", required = false) Long currentStudentId) {
        enforceStudentAccess(studentId, role, currentStudentId);
        return ResponseEntity.ok(
                service.getStudentAttendanceBetween(studentId, from, to));
    }

    // Attendance summary — overall % + per-subject % (StudentAcademics page)
    @GetMapping("/student/{studentId}/summary")
    public ResponseEntity<AttendanceSummaryResponse> getSummary(
            @PathVariable Long studentId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-Student-Id", required = false) Long currentStudentId) {
        enforceStudentAccess(studentId, role, currentStudentId);
        return ResponseEntity.ok(service.getStudentSummary(studentId));
    }

    private void enforceStudentAccess(Long studentId, String role, Long currentStudentId) {
        if ("STUDENT".equalsIgnoreCase(role)) {
            if (currentStudentId == null || !currentStudentId.equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied: You can only view your own attendance records");
            }
        }
    }

    // ── ADMIN ────────────────────────────────────────────────────

    // All attendance records
    @GetMapping("/all")
    public ResponseEntity<List<Attendance>> all() {
        return ResponseEntity.ok(service.getAllAttendance());
    }
}
