package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.LeaveApprovalRequest;
import com.successacademy.staffservice.dto.LeaveRequestDto;
import com.successacademy.staffservice.dto.LeaveResponseDto;
import com.successacademy.staffservice.model.Department;
import com.successacademy.staffservice.model.Designation;
import com.successacademy.staffservice.model.LeaveRequest;
import com.successacademy.staffservice.model.Staff;
import com.successacademy.staffservice.model.StaffAttendance;
import com.successacademy.staffservice.repository.DepartmentRepository;
import com.successacademy.staffservice.repository.DesignationRepository;
import com.successacademy.staffservice.repository.LeaveRequestRepository;
import com.successacademy.staffservice.repository.StaffAttendanceRepository;
import com.successacademy.staffservice.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
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
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRepository;
    private final StaffRepository staffRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final StaffAttendanceRepository attendanceRepository;
    private final AuditLogService auditLogService;

    @Override
    public LeaveResponseDto applyLeave(LeaveRequestDto req, Long staffId, Long actorUserId) {
        Long targetStaffId = staffId != null ? staffId : req.getStaffId();
        if (targetStaffId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff ID is required.");
        }

        Staff staff = staffRepository.findById(targetStaffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + targetStaffId));

        if (req.getFromDate() == null || req.getToDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "From date and To date are required.");
        }
        if (req.getToDate().isBefore(req.getFromDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "To date cannot be earlier than From date.");
        }

        List<LeaveRequest> overlapping = leaveRepository.findOverlappingLeaves(targetStaffId, req.getFromDate(), req.getToDate());
        if (!overlapping.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Leave request overlaps with an existing pending/approved leave.");
        }

        LeaveRequest leave = LeaveRequest.builder()
                .staffId(staff.getId())
                .leaveType(req.getLeaveType() != null ? req.getLeaveType().toUpperCase() : "CASUAL")
                .fromDate(req.getFromDate())
                .toDate(req.getToDate())
                .reason(req.getReason())
                .status("PENDING")
                .build();

        LeaveRequest saved = leaveRepository.save(leave);
        auditLogService.log(actorUserId, staff.getId(), "LEAVE_APPLIED", null, saved.getLeaveType(), "Applied leave from " + saved.getFromDate() + " to " + saved.getToDate());
        return mapToResponse(saved, staff);
    }

    @Override
    public List<LeaveResponseDto> getLeavesForStaff(Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + staffId));

        return leaveRepository.findByStaffIdOrderByCreatedAtDesc(staffId).stream()
                .map(l -> mapToResponse(l, staff))
                .toList();
    }

    @Override
    public List<LeaveResponseDto> getLeavesByStatus(String status) {
        List<LeaveRequest> list = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status))
                ? leaveRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase())
                : leaveRepository.findAll();

        return list.stream()
                .map(l -> {
                    Staff staff = staffRepository.findById(l.getStaffId()).orElse(null);
                    return mapToResponse(l, staff);
                })
                .toList();
    }

    @Override
    @Transactional
    public LeaveResponseDto processLeaveApproval(Long leaveId, LeaveApprovalRequest req, Long actorUserId) {
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found with id: " + leaveId));

        Staff staff = staffRepository.findById(leave.getStaffId()).orElse(null);

        String action = req.getAction() != null ? req.getAction().trim().toUpperCase() : "";
        if ("APPROVE".equals(action)) {
            leave.setStatus("APPROVED");
            leave.setApprovedBy(actorUserId);
            leave.setApprovedAt(LocalDateTime.now());
            leave.setRejectionReason(null);

            // Phase 2: Synchronize approved leave dates into staff_attendance
            if (leave.getFromDate() != null && leave.getToDate() != null) {
                for (LocalDate date = leave.getFromDate(); !date.isAfter(leave.getToDate()); date = date.plusDays(1)) {
                    Optional<StaffAttendance> existingOpt = attendanceRepository.findByStaffIdAndAttendanceDate(leave.getStaffId(), date);
                    if (existingOpt.isPresent()) {
                        StaffAttendance existing = existingOpt.get();
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
                        StaffAttendance att = StaffAttendance.builder()
                                .staffId(leave.getStaffId())
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
        } else if ("REJECT".equals(action)) {
            boolean wasApproved = "APPROVED".equalsIgnoreCase(leave.getStatus());
            leave.setStatus("REJECTED");
            leave.setApprovedBy(actorUserId);
            leave.setApprovedAt(LocalDateTime.now());
            leave.setRejectionReason(req.getRejectionReason());

            // Revert auto-created LEAVE records if reversing an approval
            if (wasApproved && leave.getFromDate() != null && leave.getToDate() != null) {
                for (LocalDate date = leave.getFromDate(); !date.isAfter(leave.getToDate()); date = date.plusDays(1)) {
                    Optional<StaffAttendance> existingOpt = attendanceRepository.findByStaffIdAndAttendanceDate(leave.getStaffId(), date);
                    if (existingOpt.isPresent() && "LEAVE".equalsIgnoreCase(existingOpt.get().getSource())) {
                        attendanceRepository.delete(existingOpt.get());
                    }
                }
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action. Use APPROVE or REJECT.");
        }

        LeaveRequest updated = leaveRepository.save(leave);
        auditLogService.log(actorUserId, leave.getStaffId(), "LEAVE_" + action + "D", "PENDING", action + "D", req.getRejectionReason());
        return mapToResponse(updated, staff);
    }

    private LeaveResponseDto mapToResponse(LeaveRequest l, Staff staff) {
        String staffName = staff != null ? (staff.getFirstName() + " " + staff.getLastName()).trim() : "Unknown";
        String staffCode = staff != null ? staff.getStaffCode() : "";
        String deptName = "";
        String desigName = "";

        if (staff != null) {
            if (staff.getDepartmentId() != null) {
                deptName = departmentRepository.findById(staff.getDepartmentId()).map(Department::getName).orElse("");
            }
            if (staff.getDesignationId() != null) {
                desigName = designationRepository.findById(staff.getDesignationId()).map(Designation::getName).orElse("");
            }
        }

        return LeaveResponseDto.builder()
                .id(l.getId())
                .staffId(l.getStaffId())
                .staffName(staffName)
                .staffCode(staffCode)
                .departmentName(deptName)
                .designationName(desigName)
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
