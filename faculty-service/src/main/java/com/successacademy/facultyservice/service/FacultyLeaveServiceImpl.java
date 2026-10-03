package com.successacademy.facultyservice.service;

import com.successacademy.facultyservice.dto.FacultyLeaveApprovalRequest;
import com.successacademy.facultyservice.dto.FacultyLeaveRequestDto;
import com.successacademy.facultyservice.dto.FacultyLeaveResponseDto;
import com.successacademy.facultyservice.model.Faculty;
import com.successacademy.facultyservice.model.FacultyAttendance;
import com.successacademy.facultyservice.model.FacultyLeaveRequest;
import com.successacademy.facultyservice.repository.FacultyAttendanceRepository;
import com.successacademy.facultyservice.repository.FacultyLeaveRepository;
import com.successacademy.facultyservice.repository.FacultyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacultyLeaveServiceImpl implements FacultyLeaveService {

    private final FacultyLeaveRepository leaveRepository;
    private final FacultyAttendanceRepository attendanceRepository;
    private final FacultyRepository facultyRepository;

    @Override
    @Transactional
    public FacultyLeaveResponseDto applyLeave(FacultyLeaveRequestDto req, Long actorUserId) {
        if (req.getFacultyId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faculty ID is required.");
        }
        if (req.getFromDate() == null || req.getToDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from date and to date are required.");
        }
        if (req.getToDate().isBefore(req.getFromDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "To date cannot be before from date.");
        }

        Faculty faculty = facultyRepository.findById(req.getFacultyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found with id: " + req.getFacultyId()));

        String leaveType = (req.getLeaveType() != null && !req.getLeaveType().isBlank())
                ? req.getLeaveType().trim().toUpperCase()
                : "PAID";

        FacultyLeaveRequest leave = FacultyLeaveRequest.builder()
                .facultyId(req.getFacultyId())
                .leaveType(leaveType)
                .fromDate(req.getFromDate())
                .toDate(req.getToDate())
                .reason(req.getReason())
                .status("PENDING")
                .build();

        FacultyLeaveRequest saved = leaveRepository.save(leave);
        return mapToResponse(saved, faculty);
    }

    @Override
    public List<FacultyLeaveResponseDto> getLeavesByStatus(String status) {
        List<FacultyLeaveRequest> list = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status))
                ? leaveRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase())
                : leaveRepository.findAllByOrderByCreatedAtDesc();

        return list.stream()
                .map(l -> {
                    Faculty faculty = facultyRepository.findById(l.getFacultyId()).orElse(null);
                    return mapToResponse(l, faculty);
                })
                .toList();
    }

    @Override
    public List<FacultyLeaveResponseDto> getLeavesForFaculty(Long facultyId) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty not found with id: " + facultyId));

        return leaveRepository.findByFacultyIdOrderByCreatedAtDesc(facultyId).stream()
                .map(l -> mapToResponse(l, faculty))
                .toList();
    }

    @Override
    @Transactional
    public FacultyLeaveResponseDto processLeaveApproval(Long leaveId, FacultyLeaveApprovalRequest req, Long actorUserId) {
        FacultyLeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found with id: " + leaveId));

        Faculty faculty = facultyRepository.findById(leave.getFacultyId()).orElse(null);

        String action = (req != null && req.getAction() != null) ? req.getAction().trim().toUpperCase() : "APPROVE";

        if ("APPROVE".equals(action)) {
            leave.setStatus("APPROVED");
            leave.setApprovedBy(actorUserId);
            leave.setApprovedAt(LocalDateTime.now());
            leave.setRejectionReason(null);

            // Phase 4: Synchronize approved leave dates into faculty_attendance
            if (leave.getFromDate() != null && leave.getToDate() != null) {
                for (LocalDate date = leave.getFromDate(); !date.isAfter(leave.getToDate()); date = date.plusDays(1)) {
                    Optional<FacultyAttendance> existingOpt = attendanceRepository.findByFacultyIdAndAttendanceDate(leave.getFacultyId(), date);
                    if (existingOpt.isPresent()) {
                        FacultyAttendance existing = existingOpt.get();
                        // Only auto-update if untouched admin present or previous leave
                        if ("LEAVE".equalsIgnoreCase(existing.getSource()) ||
                            ("ADMIN".equalsIgnoreCase(existing.getSource()) && "PRESENT".equalsIgnoreCase(existing.getStatus()))) {
                            existing.setStatus("LEAVE");
                            existing.setSource("LEAVE");
                            existing.setMarkedBy(actorUserId);
                            existing.setRemarks("Approved Leave: " + leave.getLeaveType() + (leave.getReason() != null ? " - " + leave.getReason() : ""));
                            attendanceRepository.save(existing);
                        }
                    } else {
                        FacultyAttendance att = FacultyAttendance.builder()
                                .facultyId(leave.getFacultyId())
                                .attendanceDate(date)
                                .status("LEAVE")
                                .source("LEAVE")
                                .markedBy(actorUserId)
                                .remarks("Approved Leave: " + leave.getLeaveType() + (leave.getReason() != null ? " - " + leave.getReason() : ""))
                                .build();
                        attendanceRepository.save(att);
                    }
                }
            }
        } else if ("REJECT".equals(action) || "CANCEL".equals(action)) {
            boolean wasApproved = "APPROVED".equalsIgnoreCase(leave.getStatus());
            leave.setStatus("CANCEL".equals(action) ? "CANCELLED" : "REJECTED");
            leave.setApprovedBy(actorUserId);
            leave.setApprovedAt(LocalDateTime.now());
            if ("REJECT".equals(action)) {
                leave.setRejectionReason(req != null ? req.getRejectionReason() : null);
            }

            // Revert auto-created LEAVE records if reversing an approval
            if (wasApproved && leave.getFromDate() != null && leave.getToDate() != null) {
                for (LocalDate date = leave.getFromDate(); !date.isAfter(leave.getToDate()); date = date.plusDays(1)) {
                    Optional<FacultyAttendance> existingOpt = attendanceRepository.findByFacultyIdAndAttendanceDate(leave.getFacultyId(), date);
                    if (existingOpt.isPresent() && "LEAVE".equalsIgnoreCase(existingOpt.get().getSource())) {
                        attendanceRepository.delete(existingOpt.get());
                    }
                }
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action. Use APPROVE, REJECT or CANCEL.");
        }

        FacultyLeaveRequest updated = leaveRepository.save(leave);
        return mapToResponse(updated, faculty);
    }

    private FacultyLeaveResponseDto mapToResponse(FacultyLeaveRequest l, Faculty f) {
        String name = (f != null) ? f.getName() : "Unknown";
        String code = (f != null && f.getFacultyCode() != null) ? f.getFacultyCode() : (f != null ? String.format("FAC-%04d", f.getId()) : "");
        String dept = (f != null && f.getDepartment() != null) ? f.getDepartment() : "Academic";
        String desig = (f != null && f.getDesignation() != null) ? f.getDesignation() : "";

        return FacultyLeaveResponseDto.builder()
                .id(l.getId())
                .facultyId(l.getFacultyId())
                .facultyName(name)
                .facultyCode(code)
                .department(dept)
                .designation(desig)
                .leaveType(l.getLeaveType())
                .fromDate(l.getFromDate())
                .toDate(l.getToDate())
                .reason(l.getReason())
                .status(l.getStatus())
                .approvedBy(l.getApprovedBy())
                .approvedAt(l.getApprovedAt())
                .rejectionReason(l.getRejectionReason())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }
}
