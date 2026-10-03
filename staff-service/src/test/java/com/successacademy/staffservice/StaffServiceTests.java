package com.successacademy.staffservice;

import com.successacademy.staffservice.client.AuthServiceClient;
import com.successacademy.staffservice.dto.*;
import com.successacademy.staffservice.model.*;
import com.successacademy.staffservice.repository.*;
import com.successacademy.staffservice.service.AuditLogService;
import com.successacademy.staffservice.service.StaffServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaffServiceTests {

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DesignationRepository designationRepository;

    @Mock
    private StaffShiftAssignmentRepository shiftAssignmentRepository;

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private StaffAttendanceRepository attendanceRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private StaffSalaryRepository salaryRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private StaffServiceImpl staffService;

    private Department dept;
    private Designation desig;

    @BeforeEach
    void setUp() {
        dept = Department.builder()
                .id(1L)
                .name("Housekeeping")
                .code("HK")
                .status("ACTIVE")
                .build();

        desig = Designation.builder()
                .id(1L)
                .name("Cleaner")
                .code("CLN")
                .departmentId(1L)
                .status("ACTIVE")
                .build();
    }

    @Test
    void testCreatePhonelessStaff_Success_NotProvisioned() {
        // Ram Kumar: No phone, no email, no ERP login
        StaffRequest req = StaffRequest.builder()
                .firstName("Ram")
                .lastName("Kumar")
                .departmentId(1L)
                .designationId(1L)
                .employmentType("FULL_TIME")
                .joiningDate(LocalDate.now())
                .createSystemAccess(false)
                .build();

        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(designationRepository.findById(1L)).thenReturn(Optional.of(desig));
        when(staffRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

        Staff savedStaff = Staff.builder()
                .id(101L)
                .staffCode("STF-0001")
                .firstName("Ram")
                .lastName("Kumar")
                .departmentId(1L)
                .designationId(1L)
                .employmentType("FULL_TIME")
                .joiningDate(LocalDate.now())
                .status("ACTIVE")
                .systemAccessStatus("NOT_PROVISIONED")
                .build();

        when(staffRepository.save(any(Staff.class))).thenReturn(savedStaff);

        StaffResponse res = staffService.createStaff(req, 1L);

        assertNotNull(res);
        assertEquals("STF-0001", res.getStaffCode());
        assertEquals("Ram", res.getFirstName());
        assertEquals("NOT_PROVISIONED", res.getSystemAccessStatus());
        assertNull(res.getUserId());
        assertNull(res.getPhone());
        assertNull(res.getEmail());

        // Verify authServiceClient was NEVER invoked for phoneless staff
        verify(authServiceClient, never()).createStaffUser(any(), any(), any(), any());
    }

    @Test
    void testCreateStaffWithLogin_Success_Provisioned() {
        // Staff with email and phone who needs ERP login
        StaffRequest req = StaffRequest.builder()
                .firstName("Sunita")
                .lastName("Verma")
                .phone("9876543210")
                .email("sunita@school.com")
                .departmentId(1L)
                .designationId(1L)
                .employmentType("FULL_TIME")
                .joiningDate(LocalDate.now())
                .createSystemAccess(true)
                .username("sunita.verma")
                .password("Password@123")
                .build();

        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(designationRepository.findById(1L)).thenReturn(Optional.of(desig));
        when(staffRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

        when(staffRepository.save(any(Staff.class))).thenAnswer(i -> {
            Staff s = i.getArgument(0);
            if (s.getId() == null) s.setId(102L);
            return s;
        });

        when(authServiceClient.createStaffUser(eq(102L), eq("sunita.verma"), eq("Password@123"), eq("sunita@school.com")))
                .thenReturn(205L);

        StaffResponse res = staffService.createStaff(req, 1L);

        assertNotNull(res);
        assertEquals(205L, res.getUserId());
        assertEquals("ACTIVE", res.getSystemAccessStatus());
    }

    @Test
    void testProvisionExistingPhonelessStaff_Success() {
        // Later, Ram Kumar gets login access
        Staff existing = Staff.builder()
                .id(101L)
                .staffCode("STF-0001")
                .firstName("Ram")
                .lastName("Kumar")
                .departmentId(1L)
                .designationId(1L)
                .status("ACTIVE")
                .systemAccessStatus("NOT_PROVISIONED")
                .build();

        when(staffRepository.findById(101L)).thenReturn(Optional.of(existing));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(designationRepository.findById(1L)).thenReturn(Optional.of(desig));

        when(authServiceClient.createStaffUser(eq(101L), eq("ram.kumar"), eq("Ram@Pass123"), any()))
                .thenReturn(301L);

        when(staffRepository.save(any(Staff.class))).thenAnswer(i -> i.getArgument(0));

        StaffProvisionRequest provReq = StaffProvisionRequest.builder()
                .username("ram.kumar")
                .password("Ram@Pass123")
                .build();

        StaffResponse res = staffService.provisionAccess(101L, provReq, 1L);

        assertNotNull(res);
        assertEquals("ACTIVE", res.getSystemAccessStatus());
        assertEquals(301L, res.getUserId());
    }
}
