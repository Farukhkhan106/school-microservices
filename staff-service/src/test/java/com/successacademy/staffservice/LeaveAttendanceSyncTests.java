package com.successacademy.staffservice;

import com.successacademy.staffservice.dto.LeaveApprovalRequest;
import com.successacademy.staffservice.dto.LeaveResponseDto;
import com.successacademy.staffservice.model.LeaveRequest;
import com.successacademy.staffservice.model.Staff;
import com.successacademy.staffservice.model.StaffAttendance;
import com.successacademy.staffservice.repository.DepartmentRepository;
import com.successacademy.staffservice.repository.DesignationRepository;
import com.successacademy.staffservice.repository.LeaveRequestRepository;
import com.successacademy.staffservice.repository.StaffAttendanceRepository;
import com.successacademy.staffservice.repository.StaffRepository;
import com.successacademy.staffservice.service.AuditLogService;
import com.successacademy.staffservice.service.LeaveServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeaveAttendanceSyncTests {

    @Mock
    private LeaveRequestRepository leaveRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DesignationRepository designationRepository;

    @Mock
    private StaffAttendanceRepository attendanceRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private LeaveServiceImpl leaveService;

    private Staff staff;
    private LeaveRequest leave;

    @BeforeEach
    void setUp() {
        staff = Staff.builder()
                .id(1L)
                .staffCode("STF-0001")
                .firstName("Suresh")
                .lastName("Verma")
                .category("Driver")
                .build();

        leave = LeaveRequest.builder()
                .id(10L)
                .staffId(1L)
                .leaveType("CASUAL")
                .fromDate(LocalDate.of(2026, 10, 5))
                .toDate(LocalDate.of(2026, 10, 6))
                .reason("Family function")
                .status("PENDING")
                .build();
    }

    @Test
    void testApproveLeave_SynchronizesToStaffAttendance() {
        when(leaveRepository.findById(10L)).thenReturn(Optional.of(leave));
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));
        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

        // When approving, attendance repo returns empty for those dates
        when(attendanceRepository.findByStaffIdAndAttendanceDate(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        LeaveApprovalRequest req = LeaveApprovalRequest.builder()
                .action("APPROVE")
                .build();

        LeaveResponseDto res = leaveService.processLeaveApproval(10L, req, 99L);

        assertEquals("APPROVED", res.getStatus());

        // Verify attendanceRepository.save was called twice (Oct 5 and Oct 6) with status LEAVE
        ArgumentCaptor<StaffAttendance> captor = ArgumentCaptor.forClass(StaffAttendance.class);
        verify(attendanceRepository, times(2)).save(captor.capture());

        for (StaffAttendance att : captor.getAllValues()) {
            assertEquals(1L, att.getStaffId());
            assertEquals("LEAVE", att.getStatus());
            assertEquals("LEAVE", att.getSource());
            assertTrue(att.getRemarks().contains("Approved Leave"));
        }
    }

    @Test
    void testRejectLeave_DoesNotCreateAttendance() {
        when(leaveRepository.findById(10L)).thenReturn(Optional.of(leave));
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));
        when(leaveRepository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

        LeaveApprovalRequest req = LeaveApprovalRequest.builder()
                .action("REJECT")
                .rejectionReason("Staff shortage")
                .build();

        LeaveResponseDto res = leaveService.processLeaveApproval(10L, req, 99L);

        assertEquals("REJECTED", res.getStatus());
        verify(attendanceRepository, never()).save(any());
    }
}
