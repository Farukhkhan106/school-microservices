package com.successacademy.facultyservice;

import com.successacademy.facultyservice.dto.*;
import com.successacademy.facultyservice.model.Faculty;
import com.successacademy.facultyservice.model.FacultyAttendance;
import com.successacademy.facultyservice.model.FacultyLeaveRequest;
import com.successacademy.facultyservice.repository.FacultyAttendanceRepository;
import com.successacademy.facultyservice.repository.FacultyLeaveRepository;
import com.successacademy.facultyservice.repository.FacultyRepository;
import com.successacademy.facultyservice.service.FacultyAttendanceServiceImpl;
import com.successacademy.facultyservice.service.FacultyLeaveServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FacultyWorkflowTests {

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private FacultyAttendanceRepository attendanceRepository;

    @Mock
    private FacultyLeaveRepository leaveRepository;

    @InjectMocks
    private FacultyAttendanceServiceImpl attendanceService;

    @InjectMocks
    private FacultyLeaveServiceImpl leaveService;

    private Faculty testFaculty;

    @BeforeEach
    void setUp() {
        testFaculty = Faculty.builder()
                .id(101L)
                .name("Prof. Test Faculty")
                .facultyCode("FAC-0101")
                .department("Science")
                .designation("Senior Physics Faculty")
                .status("Active")
                .build();
    }

    @Test
    void testDailyAttendance_CreateAndUpdateExisting() {
        when(facultyRepository.findById(101L)).thenReturn(Optional.of(testFaculty));

        LocalDate date = LocalDate.of(2026, 10, 1);
        when(attendanceRepository.findByFacultyIdAndAttendanceDate(101L, date)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(FacultyAttendance.class))).thenAnswer(invocation -> {
            FacultyAttendance a = invocation.getArgument(0);
            a.setId(1L);
            return a;
        });

        // 1. Mark initial attendance
        FacultyAttendanceRequest req = FacultyAttendanceRequest.builder()
                .facultyId(101L)
                .attendanceDate(date)
                .status("PRESENT")
                .remarks("On time")
                .build();

        FacultyAttendanceResponse res = attendanceService.markAttendance(req, 1L);
        assertNotNull(res);
        assertEquals("PRESENT", res.getStatus());
        assertEquals("Prof. Test Faculty", res.getFacultyName());

        // 2. Mark update for same date -> should update existing record, not duplicate
        FacultyAttendance existingAtt = FacultyAttendance.builder()
                .id(1L)
                .facultyId(101L)
                .attendanceDate(date)
                .status("PRESENT")
                .build();
        when(attendanceRepository.findByFacultyIdAndAttendanceDate(101L, date)).thenReturn(Optional.of(existingAtt));

        FacultyAttendanceRequest updateReq = FacultyAttendanceRequest.builder()
                .facultyId(101L)
                .attendanceDate(date)
                .status("HALF_DAY")
                .remarks("Left at noon")
                .build();

        FacultyAttendanceResponse updatedRes = attendanceService.markAttendance(updateReq, 1L);
        assertEquals("HALF_DAY", updatedRes.getStatus());
        assertEquals(1L, updatedRes.getId());
    }

    @Test
    void testLeaveApproval_AutoSyncsAttendance() {
        when(facultyRepository.findById(101L)).thenReturn(Optional.of(testFaculty));

        LocalDate from = LocalDate.of(2026, 10, 6);
        LocalDate to = LocalDate.of(2026, 10, 7);

        FacultyLeaveRequest leave = FacultyLeaveRequest.builder()
                .id(50L)
                .facultyId(101L)
                .leaveType("PAID")
                .fromDate(from)
                .toDate(to)
                .reason("Medical checkup")
                .status("PENDING")
                .build();

        when(leaveRepository.findById(50L)).thenReturn(Optional.of(leave));
        when(leaveRepository.save(any(FacultyLeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        when(attendanceRepository.findByFacultyIdAndAttendanceDate(eq(101L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        FacultyLeaveApprovalRequest approvalReq = FacultyLeaveApprovalRequest.builder()
                .action("APPROVE")
                .build();

        FacultyLeaveResponseDto resp = leaveService.processLeaveApproval(50L, approvalReq, 1L);

        assertEquals("APPROVED", resp.getStatus());
        // Verify attendanceRepository.save was called twice (for Oct 6 and Oct 7)
        verify(attendanceRepository, times(2)).save(any(FacultyAttendance.class));
    }

    @Test
    void testLeaveRejection_RollsBackAutoSyncedAttendance() {
        when(facultyRepository.findById(101L)).thenReturn(Optional.of(testFaculty));

        LocalDate from = LocalDate.of(2026, 10, 8);
        LocalDate to = LocalDate.of(2026, 10, 8);

        FacultyLeaveRequest leave = FacultyLeaveRequest.builder()
                .id(55L)
                .facultyId(101L)
                .leaveType("CASUAL")
                .fromDate(from)
                .toDate(to)
                .status("APPROVED") // was approved
                .build();

        when(leaveRepository.findById(55L)).thenReturn(Optional.of(leave));
        when(leaveRepository.save(any(FacultyLeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        FacultyAttendance autoSyncedAtt = FacultyAttendance.builder()
                .id(99L)
                .facultyId(101L)
                .attendanceDate(from)
                .status("LEAVE")
                .source("LEAVE")
                .build();

        when(attendanceRepository.findByFacultyIdAndAttendanceDate(101L, from))
                .thenReturn(Optional.of(autoSyncedAtt));

        FacultyLeaveApprovalRequest rejectReq = FacultyLeaveApprovalRequest.builder()
                .action("REJECT")
                .rejectionReason("Cancelled by admin")
                .build();

        FacultyLeaveResponseDto resp = leaveService.processLeaveApproval(55L, rejectReq, 1L);

        assertEquals("REJECTED", resp.getStatus());
        verify(attendanceRepository, times(1)).delete(autoSyncedAtt);
    }
}
