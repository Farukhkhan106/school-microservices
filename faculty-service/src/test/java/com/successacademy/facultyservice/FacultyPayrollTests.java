package com.successacademy.facultyservice;

import com.successacademy.facultyservice.dto.FacultyCalculateSalaryRequest;
import com.successacademy.facultyservice.dto.FacultyMonthlySalaryResponse;
import com.successacademy.facultyservice.dto.FacultyPaymentRequest;
import com.successacademy.facultyservice.model.*;
import com.successacademy.facultyservice.repository.*;
import com.successacademy.facultyservice.service.FacultySalaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FacultyPayrollTests {

    @Mock
    private FacultyMonthlySalaryRepository monthlySalaryRepository;

    @Mock
    private FacultySalaryPaymentRepository paymentRepository;

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private FacultyAttendanceRepository attendanceRepository;

    @Mock
    private FacultyLeaveRepository leaveRepository;

    @InjectMocks
    private FacultySalaryServiceImpl salaryService;

    private Faculty testFaculty;

    @BeforeEach
    void setUp() {
        testFaculty = Faculty.builder()
                .id(202L)
                .name("Dr. Math Faculty")
                .facultyCode("FAC-0202")
                .department("Mathematics")
                .designation("PGT Mathematics")
                .baseSalary(BigDecimal.valueOf(26000.00))
                .status("Active")
                .build();
    }

    @Test
    void testPayrollCalculation_ExactScenario() {
        // Base = 26,000, Working Days = 26 -> Per day = 1,000
        // Day 1: PRESENT -> 0 ded
        // Day 2: HALF_DAY -> 0.5 ded (500)
        // Day 3: ABSENT -> 1 ded (1,000)
        // Day 4: WEEK_OFF -> 0 ded
        // Day 5: HOLIDAY -> 0 ded
        // Day 6: PAID LEAVE -> 0 ded
        // Day 7: UNPAID LEAVE -> 1 ded (1,000)
        // Total LOP = 0.5 + 1.0 + 1.0 = 2.5 days -> Deduction = 2,500 -> Net = 23,500
        when(facultyRepository.findByStatusIgnoreCase("Active")).thenReturn(List.of(testFaculty));

        List<FacultyAttendance> attList = List.of(
                FacultyAttendance.builder().facultyId(202L).attendanceDate(LocalDate.of(2026, 10, 1)).status("PRESENT").build(),
                FacultyAttendance.builder().facultyId(202L).attendanceDate(LocalDate.of(2026, 10, 2)).status("HALF_DAY").build(),
                FacultyAttendance.builder().facultyId(202L).attendanceDate(LocalDate.of(2026, 10, 3)).status("ABSENT").build(),
                FacultyAttendance.builder().facultyId(202L).attendanceDate(LocalDate.of(2026, 10, 4)).status("WEEK_OFF").build(),
                FacultyAttendance.builder().facultyId(202L).attendanceDate(LocalDate.of(2026, 10, 5)).status("HOLIDAY").build(),
                FacultyAttendance.builder().facultyId(202L).attendanceDate(LocalDate.of(2026, 10, 6)).status("LEAVE").build(),
                FacultyAttendance.builder().facultyId(202L).attendanceDate(LocalDate.of(2026, 10, 7)).status("LEAVE").build()
        );
        when(attendanceRepository.findByFacultyIdAndYearAndMonth(202L, 2026, 10)).thenReturn(attList);

        List<FacultyLeaveRequest> leaves = List.of(
                FacultyLeaveRequest.builder().facultyId(202L).leaveType("PAID")
                        .fromDate(LocalDate.of(2026, 10, 6)).toDate(LocalDate.of(2026, 10, 6)).status("APPROVED").build(),
                FacultyLeaveRequest.builder().facultyId(202L).leaveType("UNPAID")
                        .fromDate(LocalDate.of(2026, 10, 7)).toDate(LocalDate.of(2026, 10, 7)).status("APPROVED").build()
        );
        when(leaveRepository.findApprovedLeavesOverlapping(eq(202L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(leaves);

        when(monthlySalaryRepository.findByFacultyIdAndMonthAndYear(202L, 10, 2026))
                .thenReturn(Optional.empty());

        when(monthlySalaryRepository.save(any(FacultyMonthlySalary.class))).thenAnswer(inv -> {
            FacultyMonthlySalary s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        FacultyCalculateSalaryRequest req = FacultyCalculateSalaryRequest.builder()
                .month(10)
                .year(2026)
                .workingDays(26)
                .build();

        List<FacultyMonthlySalaryResponse> results = salaryService.calculateMonthlySalaries(req, 1L);
        assertNotNull(results);
        assertEquals(1, results.size());

        FacultyMonthlySalaryResponse res = results.get(0);
        assertEquals(BigDecimal.valueOf(26000.0), res.getBaseSalary());
        assertEquals(new BigDecimal("2500.00"), res.getAttendanceDeduction());
        assertEquals(new BigDecimal("23500.00"), res.getNetSalary());
        assertEquals(new BigDecimal("23500.00"), res.getRemainingAmount());
        assertEquals("PENDING", res.getPaymentStatus());
    }

    @Test
    void testPartialPaymentWorkflow() {
        FacultyMonthlySalary sal = FacultyMonthlySalary.builder()
                .id(12L)
                .facultyId(202L)
                .month(11)
                .year(2026)
                .baseSalary(BigDecimal.valueOf(26000.00))
                .grossSalary(BigDecimal.valueOf(26000.00))
                .attendanceDeduction(BigDecimal.ZERO)
                .otherDeduction(BigDecimal.ZERO)
                .netSalary(BigDecimal.valueOf(26000.00))
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(BigDecimal.valueOf(26000.00))
                .paymentStatus("PENDING")
                .build();

        when(monthlySalaryRepository.findById(12L)).thenReturn(Optional.of(sal));
        when(facultyRepository.findById(202L)).thenReturn(Optional.of(testFaculty));
        when(paymentRepository.count()).thenReturn(0L);
        when(paymentRepository.existsByReceiptNumber(anyString())).thenReturn(false);
        when(monthlySalaryRepository.save(any(FacultyMonthlySalary.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Payment 1: 5,000 -> Paid: 5,000, Outstanding: 21,000, Status: PARTIAL
        FacultyPaymentRequest p1 = FacultyPaymentRequest.builder()
                .amount(BigDecimal.valueOf(5000.00))
                .paymentMode("CASH")
                .build();
        FacultyMonthlySalaryResponse res1 = salaryService.recordSalaryPayment(12L, p1, 1L);
        assertEquals(0, new BigDecimal("5000.00").compareTo(res1.getPaidAmount()));
        assertEquals(0, new BigDecimal("21000.00").compareTo(res1.getRemainingAmount()));
        assertEquals("PARTIAL", res1.getPaymentStatus());

        // 2. Payment 2: 10,000 -> Paid: 15,000, Outstanding: 11,000, Status: PARTIAL
        FacultyPaymentRequest p2 = FacultyPaymentRequest.builder()
                .amount(BigDecimal.valueOf(10000.00))
                .paymentMode("BANK_TRANSFER")
                .build();
        FacultyMonthlySalaryResponse res2 = salaryService.recordSalaryPayment(12L, p2, 1L);
        assertEquals(0, new BigDecimal("15000.00").compareTo(res2.getPaidAmount()));
        assertEquals(0, new BigDecimal("11000.00").compareTo(res2.getRemainingAmount()));
        assertEquals("PARTIAL", res2.getPaymentStatus());

        // 3. Test Overpayment (e.g. 15,000 > remaining 11,000) -> HTTP 400
        FacultyPaymentRequest pOver = FacultyPaymentRequest.builder()
                .amount(BigDecimal.valueOf(15000.00))
                .build();
        assertThrows(ResponseStatusException.class, () -> salaryService.recordSalaryPayment(12L, pOver, 1L));

        // 4. Test Zero Payment -> HTTP 400
        FacultyPaymentRequest pZero = FacultyPaymentRequest.builder()
                .amount(BigDecimal.ZERO)
                .build();
        assertThrows(ResponseStatusException.class, () -> salaryService.recordSalaryPayment(12L, pZero, 1L));

        // 5. Test Negative Payment -> HTTP 400
        FacultyPaymentRequest pNeg = FacultyPaymentRequest.builder()
                .amount(BigDecimal.valueOf(-500.00))
                .build();
        assertThrows(ResponseStatusException.class, () -> salaryService.recordSalaryPayment(12L, pNeg, 1L));

        // 6. Final Payment: 11,000 -> Paid: 26,000, Outstanding: 0, Status: PAID
        FacultyPaymentRequest p3 = FacultyPaymentRequest.builder()
                .amount(BigDecimal.valueOf(11000.00))
                .paymentMode("CHEQUE")
                .build();
        FacultyMonthlySalaryResponse res3 = salaryService.recordSalaryPayment(12L, p3, 1L);
        assertEquals(0, new BigDecimal("26000.00").compareTo(res3.getPaidAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(res3.getRemainingAmount()));
        assertEquals("PAID", res3.getPaymentStatus());
    }
}
