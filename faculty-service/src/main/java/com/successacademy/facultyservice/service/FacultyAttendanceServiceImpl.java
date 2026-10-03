package com.successacademy.facultyservice.service;

import com.successacademy.facultyservice.dto.FacultyAttendanceRequest;
import com.successacademy.facultyservice.dto.FacultyAttendanceResponse;
import com.successacademy.facultyservice.model.Faculty;
import com.successacademy.facultyservice.model.FacultyAttendance;
import com.successacademy.facultyservice.repository.FacultyAttendanceRepository;
import com.successacademy.facultyservice.repository.FacultyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacultyAttendanceServiceImpl implements FacultyAttendanceService {

    private final FacultyAttendanceRepository attendanceRepository;
    private final FacultyRepository facultyRepository;

    private static final Set<String> VALID_STATUSES = Set.of(
            "PRESENT", "ABSENT", "HALF_DAY", "LEAVE", "HOLIDAY", "WEEK_OFF"
    );

    @Override
    @Transactional
    public FacultyAttendanceResponse markAttendance(FacultyAttendanceRequest req, Long actorUserId) {
        if (req.getFacultyId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faculty ID is required.");
        }
        if (req.getAttendanceDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attendance date is required.");
        }
        if (req.getStatus() == null || !VALID_STATUSES.contains(req.getStatus().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attendance status. Must be one of: " + VALID_STATUSES);
        }

        Faculty faculty = facultyRepository.findById(req.getFacultyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found with id: " + req.getFacultyId()));

        String status = req.getStatus().toUpperCase();

        Optional<FacultyAttendance> existingOpt = attendanceRepository.findByFacultyIdAndAttendanceDate(
                req.getFacultyId(), req.getAttendanceDate()
        );

        FacultyAttendance attendance;
        if (existingOpt.isPresent()) {
            attendance = existingOpt.get();
            attendance.setStatus(status);
            attendance.setSource("ADMIN");
            if (req.getCheckIn() != null) attendance.setCheckIn(req.getCheckIn());
            if (req.getCheckOut() != null) attendance.setCheckOut(req.getCheckOut());
            if (req.getRemarks() != null) attendance.setRemarks(req.getRemarks());
            attendance.setMarkedBy(actorUserId);
        } else {
            attendance = FacultyAttendance.builder()
                    .facultyId(req.getFacultyId())
                    .attendanceDate(req.getAttendanceDate())
                    .status(status)
                    .source("ADMIN")
                    .checkIn(req.getCheckIn())
                    .checkOut(req.getCheckOut())
                    .remarks(req.getRemarks())
                    .markedBy(actorUserId)
                    .build();
        }

        FacultyAttendance saved = attendanceRepository.save(attendance);
        return mapToResponse(saved, faculty);
    }

    @Override
    @Transactional
    public List<FacultyAttendanceResponse> bulkMarkAttendance(List<FacultyAttendanceRequest> requests, Long actorUserId) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<FacultyAttendanceResponse> results = new ArrayList<>();
        for (FacultyAttendanceRequest req : requests) {
            results.add(markAttendance(req, actorUserId));
        }
        return results;
    }

    @Override
    public List<FacultyAttendanceResponse> getAttendanceByDate(LocalDate date, String department) {
        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        List<FacultyAttendance> list = attendanceRepository.findByAttendanceDate(queryDate);

        return list.stream()
                .map(att -> {
                    Faculty f = facultyRepository.findById(att.getFacultyId()).orElse(null);
                    return mapToResponse(att, f);
                })
                .filter(resp -> department == null || department.isBlank() || "ALL".equalsIgnoreCase(department)
                        || (resp.getDepartment() != null && resp.getDepartment().equalsIgnoreCase(department.trim())))
                .toList();
    }

    @Override
    public List<FacultyAttendanceResponse> getAttendanceByMonth(int month, int year, String department) {
        List<FacultyAttendance> list = attendanceRepository.findByYearAndMonth(year, month);

        return list.stream()
                .map(att -> {
                    Faculty f = facultyRepository.findById(att.getFacultyId()).orElse(null);
                    return mapToResponse(att, f);
                })
                .filter(resp -> department == null || department.isBlank() || "ALL".equalsIgnoreCase(department)
                        || (resp.getDepartment() != null && resp.getDepartment().equalsIgnoreCase(department.trim())))
                .toList();
    }

    @Override
    public List<FacultyAttendanceResponse> getAttendanceForFaculty(Long facultyId, LocalDate from, LocalDate to) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found with id: " + facultyId));

        List<FacultyAttendance> list;
        if (from != null && to != null) {
            list = attendanceRepository.findByFacultyIdAndAttendanceDateBetween(facultyId, from, to);
        } else {
            list = attendanceRepository.findByFacultyIdOrderByAttendanceDateDesc(facultyId);
        }

        return list.stream().map(att -> mapToResponse(att, faculty)).toList();
    }

    private FacultyAttendanceResponse mapToResponse(FacultyAttendance att, Faculty f) {
        String name = (f != null) ? f.getName() : "Unknown";
        String code = (f != null && f.getFacultyCode() != null) ? f.getFacultyCode() : (f != null ? String.format("FAC-%04d", f.getId()) : "");
        String dept = (f != null && f.getDepartment() != null) ? f.getDepartment() : "Academic";

        return FacultyAttendanceResponse.builder()
                .id(att.getId())
                .facultyId(att.getFacultyId())
                .facultyName(name)
                .facultyCode(code)
                .department(dept)
                .attendanceDate(att.getAttendanceDate())
                .status(att.getStatus())
                .source(att.getSource())
                .checkIn(att.getCheckIn())
                .checkOut(att.getCheckOut())
                .remarks(att.getRemarks())
                .markedBy(att.getMarkedBy())
                .createdAt(att.getCreatedAt())
                .updatedAt(att.getUpdatedAt())
                .build();
    }
}
