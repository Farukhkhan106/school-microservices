package com.successacademy.staffservice;

import com.successacademy.staffservice.dto.*;
import com.successacademy.staffservice.model.*;
import com.successacademy.staffservice.repository.*;
import com.successacademy.staffservice.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffWorkflowTests {

    @Mock
    private StaffAttendanceRepository attendanceRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private StaffShiftAssignmentRepository shiftAssignmentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DesignationRepository designationRepository;

    @Mock
    private LeaveRequestRepository leaveRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private StaffAttendanceServiceImpl attendanceService;

    @InjectMocks
    private LeaveServiceImpl leaveService;

    private Staff staff;
    private Shift shift;

    @BeforeEach
    void setUp() {
        staff = Staff.builder()
                .id(101L)
                .staffCode("STF-0001")
                .firstName("Ram")
                .lastName("Kumar")
                .departmentId(1L)
                .designationId(1L)
                .status("ACTIVE")
                .systemAccessStatus("ACTIVE")
                .build();

        shift = Shift.builder()
                .id(1L)
                .name("Morning Shift")
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(16, 0))
                .graceMinutes(15)
                .status("ACTIVE")
                .build();
    }

    @Test
    void testSelfCheckIn_Success() {
        LocalDate today = LocalDate.now();

        when(staffRepository.findById(101L)).thenReturn(Optional.of(staff));
        when(attendanceRepository.findByStaffIdAndAttendanceDate(101L, today)).thenReturn(Optional.empty());

        when(attendanceRepository.save(any(StaffAttendance.class))).thenAnswer(i -> {
            StaffAttendance a = i.getArgument(0);
            if (a.getId() == null) a.setId(501L);
            return a;
        });

        StaffAttendanceResponse res = attendanceService.selfCheckIn(101L, "Arrived on time");

        assertNotNull(res);
        assertEquals(101L, res.getStaffId());
        assertNotNull(res.getCheckIn());
        assertEquals("SELF", res.getSource());
    }

    @Test
    void testSelfCheckOut_Success() {
        LocalDate today = LocalDate.now();

        when(staffRepository.findById(101L)).thenReturn(Optional.of(staff));

        StaffAttendance existingAttendance = StaffAttendance.builder()
                .id(501L)
                .staffId(101L)
                .attendanceDate(today)
                .checkIn(LocalTime.of(8, 5))
                .status("PRESENT")
                .source("SELF")
                .build();

        when(attendanceRepository.findByStaffIdAndAttendanceDate(101L, today))
                .thenReturn(Optional.of(existingAttendance));

        when(attendanceRepository.save(any(StaffAttendance.class))).thenAnswer(i -> i.getArgument(0));

        StaffAttendanceResponse res = attendanceService.selfCheckOut(101L, "Completed day duty");

        assertNotNull(res);
        assertNotNull(res.getCheckOut());
    }

    @Test
    void testApplyAndApproveLeave() {
        LocalDate from = LocalDate.now().plusDays(5);
        LocalDate to = LocalDate.now().plusDays(7);

        when(staffRepository.findById(101L)).thenReturn(Optional.of(staff));
        when(leaveRepository.findOverlappingLeaves(101L, from, to)).thenReturn(List.of());

        LeaveRequestDto leaveDto = LeaveRequestDto.builder()
                .staffId(101L)
                .leaveType("CASUAL")
                .fromDate(from)
                .toDate(to)
                .reason("Personal urgent work")
                .build();

        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(i -> {
            LeaveRequest lr = i.getArgument(0);
            if (lr.getId() == null) lr.setId(601L);
            return lr;
        });

        LeaveResponseDto applied = leaveService.applyLeave(leaveDto, 101L, 101L);

        assertNotNull(applied);
        assertEquals("PENDING", applied.getStatus());

        // Approve leave
        LeaveRequest pendingLeave = LeaveRequest.builder()
                .id(601L)
                .staffId(101L)
                .leaveType("CASUAL")
                .fromDate(from)
                .toDate(to)
                .reason("Personal urgent work")
                .status("PENDING")
                .build();

        when(leaveRepository.findById(601L)).thenReturn(Optional.of(pendingLeave));

        LeaveApprovalRequest approvalReq = LeaveApprovalRequest.builder()
                .action("APPROVE")
                .build();

        LeaveResponseDto approved = leaveService.processLeaveApproval(601L, approvalReq, 1L);

        assertNotNull(approved);
        assertEquals("APPROVED", approved.getStatus());
        verify(auditLogService, times(1)).log(eq(1L), eq(101L), eq("LEAVE_APPROVED"), any(), any(), any());
    }
}
