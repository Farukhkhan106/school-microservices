package com.successacademy.attendanceservice.service;

import com.successacademy.attendanceservice.dto.AttendanceRequest;
import com.successacademy.attendanceservice.dto.AttendanceSummaryResponse;
import com.successacademy.attendanceservice.dto.BulkAttendanceRequest;
import com.successacademy.attendanceservice.model.Attendance;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    // Teacher: mark single student
    Attendance markAttendance(AttendanceRequest request);

    // Teacher: bulk save whole class at once
    List<Attendance> markBulkAttendance(BulkAttendanceRequest request);

    // Teacher: fetch existing attendance for a class+section+date (for editing)
    List<Attendance> getClassAttendance(String studentClass,
                                        String section,
                                        LocalDate date);

    // Student: full attendance history
    List<Attendance> getStudentAttendance(Long studentId);

    // Student: attendance between date range
    List<Attendance> getStudentAttendanceBetween(Long studentId,
                                                  LocalDate from,
                                                  LocalDate to);

    // Student: overall + per-subject summary (for StudentAcademics page)
    AttendanceSummaryResponse getStudentSummary(Long studentId);

    // Admin: all records
    List<Attendance> getAllAttendance();
}
