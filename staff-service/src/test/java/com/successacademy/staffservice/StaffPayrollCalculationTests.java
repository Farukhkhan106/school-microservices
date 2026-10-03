package com.successacademy.staffservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.staffservice.dto.CalculateSalaryRequest;
import com.successacademy.staffservice.dto.StaffMonthlySalaryResponse;
import com.successacademy.staffservice.dto.StaffPaymentRequest;
import com.successacademy.staffservice.model.*;
import com.successacademy.staffservice.repository.*;
import com.successacademy.staffservice.service.AuditLogService;
import com.successacademy.staffservice.service.StaffSalaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaffPayrollCalculationTests {

    @Mock
    private StaffSalaryRepository salaryRepository;

    @Mock
    private StaffMonthlySalaryRepository monthlySalaryRepository;

    @Mock
    private StaffAttendanceRepository attendanceRepository;

    @Mock
    private StaffSalaryPaymentRepository paymentRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private StaffSalaryServiceImpl salaryService;

    private Staff staff;
    private StaffSalary salaryConfig;

    @BeforeEach
    void setUp() {
        staff = Staff.builder()
                .id(1L)
                .staffCode("STF-0001")
                .firstName("Amit")
                .lastName("Sharma")
                .category("Teacher")
                .designation("Senior Teacher")
                .status("ACTIVE")
                .build();

        salaryConfig = StaffSalary.builder()
                .id(10L)
                .staffId(1L)
                .baseSalary(new BigDecimal("35000.00"))
                .monthlySalary(new BigDecimal("35000.00"))
                .transportAllowance(BigDecimal.ZERO)
                .otherAllowance(BigDecimal.ZERO)
                .fixedDeduction(BigDecimal.ZERO)
                .active(true)
                .build();
    }

    @Test
    void testOngoingMonth_UnmarkedDaysDoNotBecomeAbsences() {
        // In an ongoing month (e.g. October), only 2 days marked PRESENT, 24 unmarked days.
        // Deduction MUST be 0, and Net Salary MUST be 35,000, NOT 3,992.31!
        LocalDate oct1 = LocalDate.of(2026, 10, 1);
        LocalDate oct2 = LocalDate.of(2026, 10, 2);

        StaffAttendance att1 = StaffAttendance.builder().staffId(1L).attendanceDate(oct1).status("PRESENT").build();
        StaffAttendance att2 = StaffAttendance.builder().staffId(1L).attendanceDate(oct2).status("PRESENT").build();

        when(staffRepository.findAll()).thenReturn(List.of(staff));
        when(salaryRepository.findByStaffId(1L)).thenReturn(Optional.of(salaryConfig));
        when(monthlySalaryRepository.findByStaffIdAndMonthAndYear(1L, 10, 2026)).thenReturn(Optional.empty());
        when(attendanceRepository.findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(eq(1L), any(), any()))
                .thenReturn(List.of(att1, att2));
        when(leaveRequestRepository.findApprovedLeavesInDateRange(eq(1L), any(), any())).thenReturn(Collections.emptyList());

        when(monthlySalaryRepository.save(any(StaffMonthlySalary.class))).thenAnswer(i -> i.getArgument(0));

        CalculateSalaryRequest req = CalculateSalaryRequest.builder()
                .month(10)
                .year(2026)
                .workingDays(26)
                .build();

        List<StaffMonthlySalaryResponse> results = salaryService.calculateMonthlySalaries(req, 1L);

        assertNotNull(results);
        assertEquals(1, results.size());
        StaffMonthlySalaryResponse sal = results.get(0);

        // Core requirement verification
        assertEquals(0, new BigDecimal("35000.00").compareTo(sal.getBaseSalary()));
        assertEquals(0, new BigDecimal("35000.00").compareTo(sal.getGrossSalary()));
        assertEquals(0, BigDecimal.ZERO.compareTo(sal.getAttendanceDeduction()), "Unmarked days must NOT create attendance deduction!");
        assertEquals(0, new BigDecimal("35000.00").compareTo(sal.getNetSalary()), "Net salary must be full base salary when no confirmed absences!");
        assertEquals(2.0, sal.getPresentDays());
        assertEquals(24.0, sal.getUnmarkedDays());
        assertEquals(0.0, sal.getAbsentDays());
    }

    @Test
    void testConfirmedAbsence_DeductsCorrectly() {
        // Staff has 2 confirmed ABSENT days out of 26 working days.
        // Per day = 35000 / 26 = 1346.1538 -> 2 days = 2692.31 deduction.
        LocalDate oct1 = LocalDate.of(2026, 10, 1);
        LocalDate oct2 = LocalDate.of(2026, 10, 2);

        StaffAttendance att1 = StaffAttendance.builder().staffId(1L).attendanceDate(oct1).status("ABSENT").build();
        StaffAttendance att2 = StaffAttendance.builder().staffId(1L).attendanceDate(oct2).status("ABSENT").build();

        when(staffRepository.findAll()).thenReturn(List.of(staff));
        when(salaryRepository.findByStaffId(1L)).thenReturn(Optional.of(salaryConfig));
        when(monthlySalaryRepository.findByStaffIdAndMonthAndYear(1L, 10, 2026)).thenReturn(Optional.empty());
        when(attendanceRepository.findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(eq(1L), any(), any()))
                .thenReturn(List.of(att1, att2));
        when(leaveRequestRepository.findApprovedLeavesInDateRange(eq(1L), any(), any())).thenReturn(Collections.emptyList());

        when(monthlySalaryRepository.save(any(StaffMonthlySalary.class))).thenAnswer(i -> i.getArgument(0));

        CalculateSalaryRequest req = CalculateSalaryRequest.builder()
                .month(10)
                .year(2026)
                .workingDays(26)
                .build();

        List<StaffMonthlySalaryResponse> results = salaryService.calculateMonthlySalaries(req, 1L);
        StaffMonthlySalaryResponse sal = results.get(0);

        assertEquals(2.0, sal.getAbsentDays());
        assertTrue(sal.getAttendanceDeduction().compareTo(BigDecimal.ZERO) > 0, "Confirmed absences must deduct salary");
        assertEquals(new BigDecimal("2692.31"), sal.getAttendanceDeduction());
        assertEquals(new BigDecimal("32307.69"), sal.getNetSalary());
    }

    @Test
    void testPartialPaymentLedger_MultiplePaymentsAndOverpaymentProtection() {
        // Net Salary is 35,000.
        StaffMonthlySalary monthly = StaffMonthlySalary.builder()
                .id(100L)
                .staffId(1L)
                .month(10)
                .year(2026)
                .workingDays(26)
                .grossSalary(new BigDecimal("35000.00"))
                .attendanceDeduction(BigDecimal.ZERO)
                .otherDeduction(BigDecimal.ZERO)
                .netSalary(new BigDecimal("35000.00"))
                .paidAmount(BigDecimal.ZERO)
                .paymentStatus("PENDING")
                .locked(false)
                .build();

        when(monthlySalaryRepository.findById(100L)).thenReturn(Optional.of(monthly));
        when(staffRepository.findById(1L)).thenReturn(Optional.of(staff));
        when(paymentRepository.save(any(StaffSalaryPayment.class))).thenAnswer(i -> i.getArgument(0));
        when(monthlySalaryRepository.save(any(StaffMonthlySalary.class))).thenAnswer(i -> i.getArgument(0));

        // Payment 1: ₹1,000
        StaffPaymentRequest p1 = StaffPaymentRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .paymentMode("Cash")
                .build();

        StaffMonthlySalaryResponse res1 = salaryService.recordSalaryPayment(100L, p1, 1L);

        assertEquals(new BigDecimal("35000.00"), res1.getNetSalary(), "Net salary must never change when payment recorded!");
        assertEquals(new BigDecimal("1000.00"), res1.getPaidAmount());
        assertEquals(new BigDecimal("34000.00"), res1.getRemainingAmount());
        assertEquals("PARTIAL", res1.getPaymentStatus());

        // Payment 2: ₹10,000
        StaffPaymentRequest p2 = StaffPaymentRequest.builder()
                .amount(new BigDecimal("10000.00"))
                .paymentMode("UPI")
                .build();

        StaffMonthlySalaryResponse res2 = salaryService.recordSalaryPayment(100L, p2, 1L);

        assertEquals(new BigDecimal("35000.00"), res2.getNetSalary());
        assertEquals(new BigDecimal("11000.00"), res2.getPaidAmount());
        assertEquals(new BigDecimal("24000.00"), res2.getRemainingAmount());
        assertEquals("PARTIAL", res2.getPaymentStatus());

        // Overpayment check: trying to pay ₹25,000 when only ₹24,000 is outstanding
        StaffPaymentRequest overpay = StaffPaymentRequest.builder()
                .amount(new BigDecimal("25000.00"))
                .paymentMode("Bank Transfer")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            salaryService.recordSalaryPayment(100L, overpay, 1L);
        });
        assertTrue(ex.getReason().contains("exceeds outstanding balance"));

        // Final Payment: remaining ₹24,000
        StaffPaymentRequest p3 = StaffPaymentRequest.builder()
                .amount(new BigDecimal("24000.00"))
                .paymentMode("Bank Transfer")
                .build();

        StaffMonthlySalaryResponse res3 = salaryService.recordSalaryPayment(100L, p3, 1L);

        assertEquals(new BigDecimal("35000.00"), res3.getNetSalary());
        assertEquals(new BigDecimal("35000.00"), res3.getPaidAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(res3.getRemainingAmount()));
        assertEquals("PAID", res3.getPaymentStatus());
    }
}
