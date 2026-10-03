package com.successacademy.facultyservice;

import com.successacademy.facultyservice.dto.*;
import com.successacademy.facultyservice.model.*;
import com.successacademy.facultyservice.repository.*;
import com.successacademy.facultyservice.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FacultyComprehensiveTests {

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private FacultyAttendanceRepository attendanceRepository;

    @Mock
    private FacultyLeaveRepository leaveRepository;

    @Mock
    private FacultyMonthlySalaryRepository monthlySalaryRepository;

    @Mock
    private FacultySalaryPaymentRepository paymentRepository;

    @Mock
    private FacultyDocumentRepository documentRepository;

    @InjectMocks
    private FacultyAttendanceServiceImpl attendanceService;

    @InjectMocks
    private FacultyLeaveServiceImpl leaveService;

    @InjectMocks
    private FacultySalaryServiceImpl salaryService;

    @InjectMocks
    private FacultyDocumentServiceImpl documentService;

    private Faculty testFaculty;

    @BeforeEach
    void setUp() {
        testFaculty = Faculty.builder()
                .id(301L)
                .name("Prof. Albus Dumbledore")
                .facultyCode("FAC-0301")
                .department("Academic")
                .designation("Head of Faculty")
                .baseSalary(new BigDecimal("26000.00"))
                .status("Active")
                .build();
    }

    // 1. Faculty Attendance Creation
    @Test
    @DisplayName("1. Faculty attendance recording")
    void test1_FacultyAttendanceCreation() {
        when(facultyRepository.findById(301L)).thenReturn(Optional.of(testFaculty));
        LocalDate date = LocalDate.of(2026, 10, 1);
        when(attendanceRepository.findByFacultyIdAndAttendanceDate(301L, date)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(FacultyAttendance.class))).thenAnswer(i -> {
            FacultyAttendance a = i.getArgument(0);
            a.setId(10L);
            return a;
        });

        FacultyAttendanceRequest req = FacultyAttendanceRequest.builder()
                .facultyId(301L)
                .attendanceDate(date)
                .status("PRESENT")
                .checkIn(java.time.LocalTime.of(8, 30))
                .checkOut(java.time.LocalTime.of(16, 30))
                .remarks("Normal shift")
                .build();

        FacultyAttendanceResponse res = attendanceService.markAttendance(req, 1L);
        assertNotNull(res);
        assertEquals("PRESENT", res.getStatus());
        assertEquals("Prof. Albus Dumbledore", res.getFacultyName());
    }

    // 2. Duplicate Attendance Prevention / Update Existing
    @Test
    @DisplayName("2. Duplicate attendance prevention (updates existing record)")
    void test2_DuplicateAttendancePrevention() {
        when(facultyRepository.findById(301L)).thenReturn(Optional.of(testFaculty));
        LocalDate date = LocalDate.of(2026, 10, 1);
        FacultyAttendance existing = FacultyAttendance.builder()
                .id(10L)
                .facultyId(301L)
                .attendanceDate(date)
                .status("PRESENT")
                .build();
        when(attendanceRepository.findByFacultyIdAndAttendanceDate(301L, date)).thenReturn(Optional.of(existing));
        when(attendanceRepository.save(any(FacultyAttendance.class))).thenAnswer(i -> i.getArgument(0));

        FacultyAttendanceRequest req = FacultyAttendanceRequest.builder()
                .facultyId(301L)
                .attendanceDate(date)
                .status("HALF_DAY")
                .remarks("Updated to half day")
                .build();

        FacultyAttendanceResponse res = attendanceService.markAttendance(req, 1L);
        assertEquals(10L, res.getId());
        assertEquals("HALF_DAY", res.getStatus());
    }

    // 3. Week-Off Calculation (0 LOP)
    @Test
    @DisplayName("3. Week-off calculation results in 0 deduction")
    void test3_WeekOffCalculation() {
        when(facultyRepository.findByStatusIgnoreCase("Active")).thenReturn(List.of(testFaculty));
        List<FacultyAttendance> attList = List.of(
                FacultyAttendance.builder().facultyId(301L).attendanceDate(LocalDate.of(2026, 10, 4)).status("WEEK_OFF").build()
        );
        when(attendanceRepository.findByFacultyIdAndYearAndMonth(301L, 2026, 10)).thenReturn(attList);
        when(leaveRepository.findApprovedLeavesOverlapping(eq(301L), any(), any())).thenReturn(List.of());
        when(monthlySalaryRepository.findByFacultyIdAndMonthAndYear(301L, 10, 2026)).thenReturn(Optional.empty());
        when(monthlySalaryRepository.save(any(FacultyMonthlySalary.class))).thenAnswer(i -> i.getArgument(0));

        FacultyCalculateSalaryRequest req = FacultyCalculateSalaryRequest.builder().month(10).year(2026).workingDays(26).build();
        List<FacultyMonthlySalaryResponse> res = salaryService.calculateMonthlySalaries(req, 1L);

        assertEquals(0, BigDecimal.ZERO.compareTo(res.get(0).getAttendanceDeduction()));
    }

    // 4. Paid Leave Calculation (0 LOP)
    @Test
    @DisplayName("4. Paid leave calculation results in 0 deduction")
    void test4_PaidLeaveCalculation() {
        when(facultyRepository.findByStatusIgnoreCase("Active")).thenReturn(List.of(testFaculty));
        List<FacultyAttendance> attList = List.of(
                FacultyAttendance.builder().facultyId(301L).attendanceDate(LocalDate.of(2026, 10, 6)).status("LEAVE").build()
        );
        when(attendanceRepository.findByFacultyIdAndYearAndMonth(301L, 2026, 10)).thenReturn(attList);
        List<FacultyLeaveRequest> leaves = List.of(
                FacultyLeaveRequest.builder().facultyId(301L).leaveType("PAID").fromDate(LocalDate.of(2026, 10, 6)).toDate(LocalDate.of(2026, 10, 6)).status("APPROVED").build()
        );
        when(leaveRepository.findApprovedLeavesOverlapping(eq(301L), any(), any())).thenReturn(leaves);
        when(monthlySalaryRepository.findByFacultyIdAndMonthAndYear(301L, 10, 2026)).thenReturn(Optional.empty());
        when(monthlySalaryRepository.save(any(FacultyMonthlySalary.class))).thenAnswer(i -> i.getArgument(0));

        FacultyCalculateSalaryRequest req = FacultyCalculateSalaryRequest.builder().month(10).year(2026).workingDays(26).build();
        List<FacultyMonthlySalaryResponse> res = salaryService.calculateMonthlySalaries(req, 1L);

        assertEquals(0, BigDecimal.ZERO.compareTo(res.get(0).getAttendanceDeduction()));
        assertEquals(1, res.get(0).getPaidLeaveDays());
    }

    // 5. Unpaid Leave Calculation (1 LOP day deduction)
    @Test
    @DisplayName("5. Unpaid leave calculation results in 1 LOP day deduction")
    void test5_UnpaidLeaveCalculation() {
        when(facultyRepository.findByStatusIgnoreCase("Active")).thenReturn(List.of(testFaculty));
        List<FacultyAttendance> attList = List.of(
                FacultyAttendance.builder().facultyId(301L).attendanceDate(LocalDate.of(2026, 10, 7)).status("LEAVE").build()
        );
        when(attendanceRepository.findByFacultyIdAndYearAndMonth(301L, 2026, 10)).thenReturn(attList);
        List<FacultyLeaveRequest> leaves = List.of(
                FacultyLeaveRequest.builder().facultyId(301L).leaveType("UNPAID").fromDate(LocalDate.of(2026, 10, 7)).toDate(LocalDate.of(2026, 10, 7)).status("APPROVED").build()
        );
        when(leaveRepository.findApprovedLeavesOverlapping(eq(301L), any(), any())).thenReturn(leaves);
        when(monthlySalaryRepository.findByFacultyIdAndMonthAndYear(301L, 10, 2026)).thenReturn(Optional.empty());
        when(monthlySalaryRepository.save(any(FacultyMonthlySalary.class))).thenAnswer(i -> i.getArgument(0));

        FacultyCalculateSalaryRequest req = FacultyCalculateSalaryRequest.builder().month(10).year(2026).workingDays(26).build();
        List<FacultyMonthlySalaryResponse> res = salaryService.calculateMonthlySalaries(req, 1L);

        // 1 LOP day * 1000 = 1000 deduction
        assertEquals(0, new BigDecimal("1000.00").compareTo(res.get(0).getAttendanceDeduction()));
        assertEquals(1, res.get(0).getUnpaidLeaveDays());
    }

    // 6. Leave → Attendance Synchronization
    @Test
    @DisplayName("6. Leave approval synchronizes attendance with source=LEAVE")
    void test6_LeaveToAttendanceSync() {
        when(facultyRepository.findById(301L)).thenReturn(Optional.of(testFaculty));
        LocalDate date = LocalDate.of(2026, 10, 15);
        FacultyLeaveRequest leave = FacultyLeaveRequest.builder()
                .id(77L).facultyId(301L).leaveType("PAID").fromDate(date).toDate(date).status("PENDING").build();
        when(leaveRepository.findById(77L)).thenReturn(Optional.of(leave));
        when(leaveRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(attendanceRepository.findByFacultyIdAndAttendanceDate(301L, date)).thenReturn(Optional.empty());

        leaveService.processLeaveApproval(77L, FacultyLeaveApprovalRequest.builder().action("APPROVE").build(), 1L);

        verify(attendanceRepository, times(1)).save(argThat(att ->
                att.getStatus().equals("LEAVE") && att.getSource().equals("LEAVE")
        ));
    }

    // 7. Leave Rejection (No attendance created)
    @Test
    @DisplayName("7. Leave rejection before approval creates zero attendance")
    void test7_LeaveRejectionNoAttendance() {
        FacultyLeaveRequest leave = FacultyLeaveRequest.builder()
                .id(78L).facultyId(301L).leaveType("PAID").fromDate(LocalDate.of(2026, 10, 20)).toDate(LocalDate.of(2026, 10, 20)).status("PENDING").build();
        when(leaveRepository.findById(78L)).thenReturn(Optional.of(leave));
        when(leaveRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        leaveService.processLeaveApproval(78L, FacultyLeaveApprovalRequest.builder().action("REJECT").rejectionReason("Staff shortage").build(), 1L);

        verify(attendanceRepository, never()).save(any());
    }

    // 8. Leave Rollback
    @Test
    @DisplayName("8. Leave rollback reverts only auto-synced LEAVE attendance")
    void test8_LeaveRollback() {
        LocalDate date = LocalDate.of(2026, 10, 22);
        FacultyLeaveRequest approvedLeave = FacultyLeaveRequest.builder()
                .id(79L).facultyId(301L).leaveType("PAID").fromDate(date).toDate(date).status("APPROVED").build();
        when(leaveRepository.findById(79L)).thenReturn(Optional.of(approvedLeave));
        when(leaveRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FacultyAttendance autoSynced = FacultyAttendance.builder()
                .id(555L).facultyId(301L).attendanceDate(date).status("LEAVE").source("LEAVE").build();
        when(attendanceRepository.findByFacultyIdAndAttendanceDate(301L, date)).thenReturn(Optional.of(autoSynced));

        leaveService.processLeaveApproval(79L, FacultyLeaveApprovalRequest.builder().action("REJECT").rejectionReason("Cancelled").build(), 1L);

        verify(attendanceRepository, times(1)).delete(autoSynced);
    }

    // 9. Payroll Calculation
    @Test
    @DisplayName("9. Payroll calculation: Base 26000, LOP 2.5 -> Net 23500")
    void test9_PayrollCalculation() {
        when(facultyRepository.findByStatusIgnoreCase("Active")).thenReturn(List.of(testFaculty));
        List<FacultyAttendance> attList = List.of(
                FacultyAttendance.builder().facultyId(301L).attendanceDate(LocalDate.of(2026, 10, 1)).status("PRESENT").build(),
                FacultyAttendance.builder().facultyId(301L).attendanceDate(LocalDate.of(2026, 10, 2)).status("HALF_DAY").build(),
                FacultyAttendance.builder().facultyId(301L).attendanceDate(LocalDate.of(2026, 10, 3)).status("ABSENT").build(),
                FacultyAttendance.builder().facultyId(301L).attendanceDate(LocalDate.of(2026, 10, 4)).status("WEEK_OFF").build(),
                FacultyAttendance.builder().facultyId(301L).attendanceDate(LocalDate.of(2026, 10, 5)).status("HOLIDAY").build(),
                FacultyAttendance.builder().facultyId(301L).attendanceDate(LocalDate.of(2026, 10, 6)).status("LEAVE").build(),
                FacultyAttendance.builder().facultyId(301L).attendanceDate(LocalDate.of(2026, 10, 7)).status("LEAVE").build()
        );
        when(attendanceRepository.findByFacultyIdAndYearAndMonth(301L, 2026, 10)).thenReturn(attList);
        List<FacultyLeaveRequest> leaves = List.of(
                FacultyLeaveRequest.builder().facultyId(301L).leaveType("PAID").fromDate(LocalDate.of(2026, 10, 6)).toDate(LocalDate.of(2026, 10, 6)).status("APPROVED").build(),
                FacultyLeaveRequest.builder().facultyId(301L).leaveType("UNPAID").fromDate(LocalDate.of(2026, 10, 7)).toDate(LocalDate.of(2026, 10, 7)).status("APPROVED").build()
        );
        when(leaveRepository.findApprovedLeavesOverlapping(eq(301L), any(), any())).thenReturn(leaves);
        when(monthlySalaryRepository.findByFacultyIdAndMonthAndYear(301L, 10, 2026)).thenReturn(Optional.empty());
        when(monthlySalaryRepository.save(any(FacultyMonthlySalary.class))).thenAnswer(i -> i.getArgument(0));

        FacultyCalculateSalaryRequest req = FacultyCalculateSalaryRequest.builder().month(10).year(2026).workingDays(26).build();
        List<FacultyMonthlySalaryResponse> res = salaryService.calculateMonthlySalaries(req, 1L);

        assertEquals(0, new BigDecimal("2500.00").compareTo(res.get(0).getAttendanceDeduction()));
        assertEquals(0, new BigDecimal("23500.00").compareTo(res.get(0).getNetSalary()));
    }

    // 10, 11, 12, 13, 14, 15: Payment Workflow, Validations, Status Transitions
    @Test
    @DisplayName("10-15. Partial payment, final payment, validations (overpayment, zero, negative), and status transitions")
    void test10to15_PaymentWorkflowAndValidations() {
        FacultyMonthlySalary salary = FacultyMonthlySalary.builder()
                .id(88L)
                .facultyId(301L)
                .month(10)
                .year(2026)
                .baseSalary(new BigDecimal("26000.00"))
                .netSalary(new BigDecimal("23500.00"))
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("23500.00"))
                .paymentStatus("PENDING")
                .build();

        when(monthlySalaryRepository.findById(88L)).thenReturn(Optional.of(salary));
        when(facultyRepository.findById(301L)).thenReturn(Optional.of(testFaculty));
        when(paymentRepository.count()).thenReturn(0L);
        when(paymentRepository.existsByReceiptNumber(anyString())).thenReturn(false);
        when(monthlySalaryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // 10. Partial payment 1: 5,000 -> Status PARTIAL
        FacultyMonthlySalaryResponse r1 = salaryService.recordSalaryPayment(88L, FacultyPaymentRequest.builder().amount(new BigDecimal("5000.00")).paymentMode("UPI").build(), 1L);
        assertEquals(0, new BigDecimal("5000.00").compareTo(r1.getPaidAmount()));
        assertEquals(0, new BigDecimal("18500.00").compareTo(r1.getRemainingAmount()));
        assertEquals("PARTIAL", r1.getPaymentStatus());

        // Partial payment 2: 10,000 -> Status PARTIAL
        FacultyMonthlySalaryResponse r2 = salaryService.recordSalaryPayment(88L, FacultyPaymentRequest.builder().amount(new BigDecimal("10000.00")).paymentMode("NEFT").build(), 1L);
        assertEquals(0, new BigDecimal("15000.00").compareTo(r2.getPaidAmount()));
        assertEquals(0, new BigDecimal("8500.00").compareTo(r2.getRemainingAmount()));
        assertEquals("PARTIAL", r2.getPaymentStatus());

        // 12. Overpayment rejection: 15,000 > outstanding 8,500 -> HTTP 400
        assertThrows(ResponseStatusException.class, () ->
                salaryService.recordSalaryPayment(88L, FacultyPaymentRequest.builder().amount(new BigDecimal("15000.00")).build(), 1L)
        );

        // 13. Zero payment rejection -> HTTP 400
        assertThrows(ResponseStatusException.class, () ->
                salaryService.recordSalaryPayment(88L, FacultyPaymentRequest.builder().amount(BigDecimal.ZERO).build(), 1L)
        );

        // 14. Negative payment rejection -> HTTP 400
        assertThrows(ResponseStatusException.class, () ->
                salaryService.recordSalaryPayment(88L, FacultyPaymentRequest.builder().amount(new BigDecimal("-500.00")).build(), 1L)
        );

        // 11 & 15. Final payment: 8,500 -> Status PAID, outstanding = 0
        FacultyMonthlySalaryResponse r3 = salaryService.recordSalaryPayment(88L, FacultyPaymentRequest.builder().amount(new BigDecimal("8500.00")).paymentMode("BANK_TRANSFER").build(), 1L);
        assertEquals(0, new BigDecimal("23500.00").compareTo(r3.getPaidAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(r3.getRemainingAmount()));
        assertEquals("PAID", r3.getPaymentStatus());
    }

    // 16. Document Validation (Size limit & empty file)
    @Test
    @DisplayName("16. Document validation: file size limit (< 10 MB) and empty file rejection")
    void test16_DocumentValidation() {
        // Empty file -> HTTP 400
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);
        assertThrows(ResponseStatusException.class, () ->
                documentService.uploadDocument(301L, "Aadhaar", "Aadhaar Card", "1234-5678", emptyFile, 1L)
        );

        // Oversized file (> 10 MB) -> HTTP 400
        byte[] oversizedBytes = new byte[11 * 1024 * 1024]; // 11 MB
        MockMultipartFile oversizedFile = new MockMultipartFile("file", "large.pdf", "application/pdf", oversizedBytes);
        assertThrows(ResponseStatusException.class, () ->
                documentService.uploadDocument(301L, "Aadhaar", "Aadhaar Card", "1234-5678", oversizedFile, 1L)
        );
    }

    // 17. Salary History Retrieval
    @Test
    @DisplayName("17. Faculty salary history retrieval returns historical salary records")
    void test17_SalaryHistoryRetrieval() {
        when(facultyRepository.findById(301L)).thenReturn(Optional.of(testFaculty));
        FacultyMonthlySalary s1 = FacultyMonthlySalary.builder()
                .id(101L).facultyId(301L).month(9).year(2026).baseSalary(new BigDecimal("26000.00")).netSalary(new BigDecimal("26000.00")).paymentStatus("PAID").build();
        FacultyMonthlySalary s2 = FacultyMonthlySalary.builder()
                .id(102L).facultyId(301L).month(10).year(2026).baseSalary(new BigDecimal("26000.00")).netSalary(new BigDecimal("23500.00")).paymentStatus("PARTIAL").build();

        when(monthlySalaryRepository.findByFacultyIdOrderByYearDescMonthDesc(301L)).thenReturn(List.of(s2, s1));

        List<FacultyMonthlySalaryResponse> history = salaryService.getSalaryHistory(301L);
        assertNotNull(history);
        assertEquals(2, history.size());
        assertEquals(10, history.get(0).getMonth());
        assertEquals(9, history.get(1).getMonth());
        assertEquals("Prof. Albus Dumbledore", history.get(0).getFacultyName());
    }
}
