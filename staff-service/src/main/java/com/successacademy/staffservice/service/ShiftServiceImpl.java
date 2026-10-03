package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.ShiftAssignmentRequest;
import com.successacademy.staffservice.dto.ShiftAssignmentResponse;
import com.successacademy.staffservice.dto.ShiftRequest;
import com.successacademy.staffservice.dto.ShiftResponse;
import com.successacademy.staffservice.model.Shift;
import com.successacademy.staffservice.model.Staff;
import com.successacademy.staffservice.model.StaffShiftAssignment;
import com.successacademy.staffservice.repository.ShiftRepository;
import com.successacademy.staffservice.repository.StaffRepository;
import com.successacademy.staffservice.repository.StaffShiftAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;
    private final StaffShiftAssignmentRepository assignmentRepository;
    private final StaffRepository staffRepository;
    private final AuditLogService auditLogService;

    @Override
    public List<ShiftResponse> getAllShifts(boolean activeOnly) {
        List<Shift> list = activeOnly
                ? shiftRepository.findByStatusIgnoreCase("ACTIVE")
                : shiftRepository.findAll();

        return list.stream().map(this::mapToResponse).toList();
    }

    @Override
    public ShiftResponse getShiftById(Long id) {
        Shift s = shiftRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found with id: " + id));
        return mapToResponse(s);
    }

    @Override
    public ShiftResponse createShift(ShiftRequest req, Long actorUserId) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shift name is required.");
        }
        if (req.getStartTime() == null || req.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start and End times are required.");
        }

        Shift shift = Shift.builder()
                .name(req.getName().trim())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .graceMinutes(req.getGraceMinutes() > 0 ? req.getGraceMinutes() : 15)
                .workingDays((req.getWorkingDays() != null && !req.getWorkingDays().isBlank())
                        ? req.getWorkingDays() : "Mon,Tue,Wed,Thu,Fri,Sat")
                .status((req.getStatus() != null && !req.getStatus().isBlank()) ? req.getStatus().toUpperCase() : "ACTIVE")
                .build();

        Shift saved = shiftRepository.save(shift);
        auditLogService.log(actorUserId, null, "SHIFT_CREATED", null, saved.getName(), "Created shift " + saved.getName());
        return mapToResponse(saved);
    }

    @Override
    public ShiftResponse updateShift(Long id, ShiftRequest req, Long actorUserId) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found with id: " + id));

        if (req.getName() != null && !req.getName().isBlank()) shift.setName(req.getName().trim());
        if (req.getStartTime() != null) shift.setStartTime(req.getStartTime());
        if (req.getEndTime() != null) shift.setEndTime(req.getEndTime());
        if (req.getGraceMinutes() > 0) shift.setGraceMinutes(req.getGraceMinutes());
        if (req.getWorkingDays() != null) shift.setWorkingDays(req.getWorkingDays());
        if (req.getStatus() != null) shift.setStatus(req.getStatus().toUpperCase());

        Shift updated = shiftRepository.save(shift);
        auditLogService.log(actorUserId, null, "SHIFT_UPDATED", null, updated.getName(), "Updated shift " + updated.getName());
        return mapToResponse(updated);
    }

    @Override
    public ShiftAssignmentResponse assignShiftToStaff(ShiftAssignmentRequest req, Long actorUserId) {
        if (req.getStaffId() == null || req.getShiftId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff ID and Shift ID are required.");
        }

        Staff staff = staffRepository.findById(req.getStaffId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + req.getStaffId()));

        Shift shift = shiftRepository.findById(req.getShiftId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found with id: " + req.getShiftId()));

        LocalDate effectiveFrom = req.getEffectiveFrom() != null ? req.getEffectiveFrom() : LocalDate.now();

        // Close any currently open shift assignment as of (effectiveFrom - 1 day)
        List<StaffShiftAssignment> currentAssignments = assignmentRepository.findByStaffIdOrderByEffectiveFromDesc(staff.getId());
        for (StaffShiftAssignment existing : currentAssignments) {
            if (existing.getEffectiveTo() == null || existing.getEffectiveTo().isAfter(effectiveFrom)) {
                existing.setEffectiveTo(effectiveFrom.minusDays(1));
                assignmentRepository.save(existing);
            }
        }

        StaffShiftAssignment newAssignment = StaffShiftAssignment.builder()
                .staffId(staff.getId())
                .shiftId(shift.getId())
                .effectiveFrom(effectiveFrom)
                .effectiveTo(req.getEffectiveTo())
                .assignedBy(actorUserId)
                .build();

        StaffShiftAssignment saved = assignmentRepository.save(newAssignment);
        auditLogService.log(actorUserId, staff.getId(), "SHIFT_ASSIGNED", null, shift.getName(), "Assigned to shift " + shift.getName());
        return mapAssignmentToResponse(saved, staff, shift);
    }

    @Override
    public List<ShiftAssignmentResponse> getShiftAssignmentsForStaff(Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + staffId));

        return assignmentRepository.findByStaffIdOrderByEffectiveFromDesc(staffId).stream()
                .map(a -> {
                    Shift s = shiftRepository.findById(a.getShiftId()).orElse(null);
                    return mapAssignmentToResponse(a, staff, s);
                })
                .toList();
    }

    @Override
    public ShiftAssignmentResponse getCurrentShiftForStaff(Long staffId, LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        Staff staff = staffRepository.findById(staffId).orElse(null);
        if (staff == null) return null;

        Optional<StaffShiftAssignment> active = assignmentRepository.findActiveShiftForDate(staffId, queryDate);
        if (active.isPresent()) {
            Shift s = shiftRepository.findById(active.get().getShiftId()).orElse(null);
            return mapAssignmentToResponse(active.get(), staff, s);
        }
        return null;
    }

    private ShiftResponse mapToResponse(Shift s) {
        long count = assignmentRepository.countByShiftIdAndEffectiveToIsNull(s.getId());
        return ShiftResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .graceMinutes(s.getGraceMinutes())
                .workingDays(s.getWorkingDays())
                .status(s.getStatus())
                .assignedStaffCount(count)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private ShiftAssignmentResponse mapAssignmentToResponse(StaffShiftAssignment a, Staff staff, Shift s) {
        String staffName = staff != null ? (staff.getFirstName() + " " + staff.getLastName()).trim() : "Unknown";
        String staffCode = staff != null ? staff.getStaffCode() : "";

        return ShiftAssignmentResponse.builder()
                .id(a.getId())
                .staffId(a.getStaffId())
                .staffName(staffName)
                .staffCode(staffCode)
                .shiftId(a.getShiftId())
                .shiftName(s != null ? s.getName() : "Unknown Shift")
                .startTime(s != null ? s.getStartTime() : null)
                .endTime(s != null ? s.getEndTime() : null)
                .effectiveFrom(a.getEffectiveFrom())
                .effectiveTo(a.getEffectiveTo())
                .build();
    }
}
