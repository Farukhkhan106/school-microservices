package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.StaffAttendanceRequest;
import com.successacademy.staffservice.dto.StaffAttendanceResponse;
import com.successacademy.staffservice.model.Department;
import com.successacademy.staffservice.model.Designation;
import com.successacademy.staffservice.model.Staff;
import com.successacademy.staffservice.model.StaffAttendance;
import com.successacademy.staffservice.repository.DepartmentRepository;
import com.successacademy.staffservice.repository.DesignationRepository;
import com.successacademy.staffservice.repository.StaffAttendanceRepository;
import com.successacademy.staffservice.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StaffAttendanceServiceImpl implements StaffAttendanceService {

    private final StaffAttendanceRepository attendanceRepository;
    private final StaffRepository staffRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final AuditLogService auditLogService;

    @Override
    public StaffAttendanceResponse markAttendance(StaffAttendanceRequest req, Long actorUserId) {
        if (req.getStaffId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff ID is required.");
        }
        Staff staff = staffRepository.findById(req.getStaffId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + req.getStaffId()));

        LocalDate date = req.getAttendanceDate() != null ? req.getAttendanceDate() : LocalDate.now();
        String status = req.getStatus() != null ? req.getStatus().trim().toUpperCase() : "PRESENT";

        Optional<StaffAttendance> existingOpt = attendanceRepository.findByStaffIdAndAttendanceDate(staff.getId(), date);
        StaffAttendance attendance;
        String oldStatus = null;

        if (existingOpt.isPresent()) {
            attendance = existingOpt.get();
            oldStatus = attendance.getStatus();
            attendance.setStatus(status);
            if (req.getCheckIn() != null) attendance.setCheckIn(req.getCheckIn());
            if (req.getCheckOut() != null) attendance.setCheckOut(req.getCheckOut());
            if (req.getRemarks() != null) attendance.setRemarks(req.getRemarks());
            if (req.getSource() != null) attendance.setSource(req.getSource());
            attendance.setMarkedBy(actorUserId);
        } else {
            attendance = StaffAttendance.builder()
                    .staffId(staff.getId())
                    .attendanceDate(date)
                    .status(status)
                    .checkIn(req.getCheckIn())
                    .checkOut(req.getCheckOut())
                    .source(req.getSource() != null ? req.getSource() : "ADMIN")
                    .markedBy(actorUserId)
                    .remarks(req.getRemarks())
                    .build();
        }

        StaffAttendance saved = attendanceRepository.save(attendance);
        auditLogService.log(actorUserId, staff.getId(), "ATTENDANCE_MARKED", oldStatus, saved.getStatus(),
                "Marked " + saved.getStatus() + " for date " + date);
        return mapToResponse(saved, staff);
    }

    @Override
    public List<StaffAttendanceResponse> bulkMarkAttendance(List<StaffAttendanceRequest> requests, Long actorUserId) {
        List<StaffAttendanceResponse> responses = new ArrayList<>();
        for (StaffAttendanceRequest req : requests) {
            responses.add(markAttendance(req, actorUserId));
        }
        return responses;
    }

    @Override
    public List<StaffAttendanceResponse> getAttendanceByDate(LocalDate date, Long departmentId) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        List<StaffAttendance> records = attendanceRepository.findByAttendanceDate(queryDate);

        return records.stream()
                .filter(a -> {
                    if (departmentId == null) return true;
                    Staff staff = staffRepository.findById(a.getStaffId()).orElse(null);
                    return staff != null && departmentId.equals(staff.getDepartmentId());
                })
                .map(a -> {
                    Staff staff = staffRepository.findById(a.getStaffId()).orElse(null);
                    return mapToResponse(a, staff);
                })
                .toList();
    }

    @Override
    public List<StaffAttendanceResponse> getAttendanceByMonth(int month, int year, Long departmentId) {
        java.time.YearMonth ym = java.time.YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        List<StaffAttendance> records = attendanceRepository.findByAttendanceDateBetweenOrderByAttendanceDateAsc(from, to);

        return records.stream()
                .filter(a -> {
                    if (departmentId == null) return true;
                    Staff staff = staffRepository.findById(a.getStaffId()).orElse(null);
                    return staff != null && departmentId.equals(staff.getDepartmentId());
                })
                .map(a -> {
                    Staff staff = staffRepository.findById(a.getStaffId()).orElse(null);
                    return mapToResponse(a, staff);
                })
                .toList();
    }

    @Override
    public List<StaffAttendanceResponse> getAttendanceForStaff(Long staffId, LocalDate from, LocalDate to) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + staffId));

        List<StaffAttendance> list;
        if (from != null && to != null) {
            list = attendanceRepository.findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(staffId, from, to);
        } else {
            list = attendanceRepository.findByStaffIdOrderByAttendanceDateDesc(staffId);
        }

        return list.stream().map(a -> mapToResponse(a, staff)).toList();
    }

    @Override
    public StaffAttendanceResponse selfCheckIn(Long staffId, String remarks) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + staffId));

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        Optional<StaffAttendance> existingOpt = attendanceRepository.findByStaffIdAndAttendanceDate(staffId, today);
        if (existingOpt.isPresent() && existingOpt.get().getCheckIn() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already checked in today at " + existingOpt.get().getCheckIn());
        }

        StaffAttendance attendance = existingOpt.orElseGet(() -> StaffAttendance.builder()
                .staffId(staffId)
                .attendanceDate(today)
                .build());

        attendance.setStatus("PRESENT");
        attendance.setCheckIn(now);
        attendance.setSource("SELF");
        if (remarks != null && !remarks.isBlank()) attendance.setRemarks(remarks);

        StaffAttendance saved = attendanceRepository.save(attendance);
        auditLogService.log(null, staffId, "SELF_CHECK_IN", null, now.toString(), "Self check-in");
        return mapToResponse(saved, staff);
    }

    @Override
    public StaffAttendanceResponse selfCheckOut(Long staffId, String remarks) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + staffId));

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        StaffAttendance attendance = attendanceRepository.findByStaffIdAndAttendanceDate(staffId, today)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot check out before checking in today."));

        if (attendance.getCheckIn() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot check out before checking in.");
        }
        if (attendance.getCheckOut() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already checked out today at " + attendance.getCheckOut());
        }

        attendance.setCheckOut(now);
        if (remarks != null && !remarks.isBlank()) {
            String curr = attendance.getRemarks();
            attendance.setRemarks(curr != null ? curr + " | Out: " + remarks : remarks);
        }

        StaffAttendance saved = attendanceRepository.save(attendance);
        auditLogService.log(null, staffId, "SELF_CHECK_OUT", null, now.toString(), "Self check-out");
        return mapToResponse(saved, staff);
    }

    @Override
    public StaffAttendanceResponse getTodayAttendance(Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + staffId));

        return attendanceRepository.findByStaffIdAndAttendanceDate(staffId, LocalDate.now())
                .map(a -> mapToResponse(a, staff))
                .orElse(null);
    }

    private StaffAttendanceResponse mapToResponse(StaffAttendance a, Staff staff) {
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

        return StaffAttendanceResponse.builder()
                .id(a.getId())
                .staffId(a.getStaffId())
                .staffName(staffName)
                .staffCode(staffCode)
                .departmentName(deptName)
                .designationName(desigName)
                .attendanceDate(a.getAttendanceDate())
                .status(a.getStatus())
                .checkIn(a.getCheckIn())
                .checkOut(a.getCheckOut())
                .source(a.getSource())
                .markedBy(a.getMarkedBy())
                .remarks(a.getRemarks())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
