package com.successacademy.attendanceservice.service;

import com.successacademy.attendanceservice.dto.AttendanceRequest;
import com.successacademy.attendanceservice.dto.AttendanceSummaryResponse;
import com.successacademy.attendanceservice.dto.BulkAttendanceRequest;
import com.successacademy.attendanceservice.model.Attendance;
import com.successacademy.attendanceservice.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository repository;

    // ── MARK SINGLE ─────────────────────────────────────────────
    @Override
    public Attendance markAttendance(AttendanceRequest req) {
        // Daily attendance: always enforce "General" representing ONE daily class roll call per student per date
        String subject = "General";
        Optional<Attendance> existing =
                repository.findByStudentIdAndDateAndSubject(
                        req.getStudentId(), req.getDate(), subject);

        Attendance attendance = existing.orElseGet(Attendance::new);
        attendance.setStudentId(req.getStudentId());
        attendance.setStudentName(req.getStudentName());
        attendance.setStudentClass(req.getStudentClass());
        attendance.setSection(req.getSection());
        attendance.setDate(req.getDate());
        attendance.setStatus(req.getStatus().toUpperCase());
        attendance.setSubject(subject);
        attendance.setMarkedBy(req.getMarkedBy());

        return repository.save(attendance);
    }

    // ── MARK BULK (whole class) ──────────────────────────────────
    @Override
    public List<Attendance> markBulkAttendance(BulkAttendanceRequest request) {
        return request.getRecords()
                .stream()
                .map(this::markAttendance)
                .toList();
    }

    // ── TEACHER: fetch existing for a class+section+date ────────
    @Override
    public List<Attendance> getClassAttendance(String studentClass,
                                                String section,
                                                LocalDate date) {
        if (section != null && !section.isBlank()) {
            return repository.findByStudentClassAndSectionAndDate(
                    studentClass, section, date);
        }
        return repository.findByStudentClassAndDate(studentClass, date);
    }

    // ── STUDENT: full history ────────────────────────────────────
    @Override
    public List<Attendance> getStudentAttendance(Long studentId) {
        return repository.findByStudentId(studentId)
                .stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .toList();
    }

    // ── STUDENT: history between dates ──────────────────────────
    @Override
    public List<Attendance> getStudentAttendanceBetween(Long studentId,
                                                         LocalDate from,
                                                         LocalDate to) {
        return repository.findByStudentIdAndDateBetween(studentId, from, to)
                .stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .toList();
    }

    // ── STUDENT: summary ────────────────────────────────────────
    @Override
    public AttendanceSummaryResponse getStudentSummary(Long studentId) {
        long total   = repository.countByStudentId(studentId);
        long present = repository.countByStudentIdAndStatus(studentId, "PRESENT");
        long absent  = repository.countByStudentIdAndStatus(studentId, "ABSENT");
        long late    = repository.countByStudentIdAndStatus(studentId, "LATE");

        // Daily Attendance % = (PRESENT + LATE) / TOTAL MARKED * 100
        double percentage = total > 0
                ? Math.round(((present + late) * 100.0 / total) * 10.0) / 10.0
                : 0.0;

        return AttendanceSummaryResponse.builder()
                .studentId(studentId)
                .totalDays(total)
                .presentDays(present)
                .absentDays(absent)
                .lateDays(late)
                .overallPercentage(percentage)
                .subjectWisePercentage(Map.of())
                .build();
    }

    // ── ADMIN: all records ───────────────────────────────────────
    @Override
    public List<Attendance> getAllAttendance() {
        return repository.findAll()
                .stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .toList();
    }
}
