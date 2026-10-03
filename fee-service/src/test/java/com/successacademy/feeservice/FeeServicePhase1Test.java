package com.successacademy.feeservice;

import com.successacademy.feeservice.dto.PaymentRequest;
import com.successacademy.feeservice.dto.PaymentResponse;
import com.successacademy.feeservice.dto.ReceiptResponse;
import com.successacademy.feeservice.exception.ConflictException;
import com.successacademy.feeservice.exception.ForbiddenException;
import com.successacademy.feeservice.exception.NotFoundException;
import com.successacademy.feeservice.exception.UnauthorizedException;
import com.successacademy.feeservice.model.FeePayment;
import com.successacademy.feeservice.model.FeeRecord;
import com.successacademy.feeservice.repository.FeePaymentRepository;
import com.successacademy.feeservice.repository.FeeRecordRepository;
import com.successacademy.feeservice.repository.FeeStructureRepository;
import com.successacademy.feeservice.security.UserContext;
import com.successacademy.feeservice.service.FeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeServicePhase1Test {

    @Mock
    private FeeRecordRepository feeRecordRepository;

    @Mock
    private FeePaymentRepository feePaymentRepository;

    @Mock
    private FeeStructureRepository feeStructureRepository;

    @InjectMocks
    private FeeServiceImpl feeService;

    private FeeRecord sampleRecord;

    @BeforeEach
    void setUp() {
        sampleRecord = FeeRecord.builder()
                .id(100L)
                .studentId(50L)
                .studentName("Aarav Mehta")
                .studentClass("10-A")
                .feeType("Tuition")
                .amount(BigDecimal.valueOf(25000))
                .paidAmount(BigDecimal.valueOf(10000))
                .dueDate(LocalDate.of(2026, 12, 10))
                .status("Partial")
                .build();
    }

    // ── PAYMENT VALIDATION TESTS ─────────────────────────────

    @Test
    @DisplayName("Recording payment should validate and reject payment > outstanding balance")
    void recordPayment_ExceedsOutstanding_ShouldThrowConflict() {
        when(feeRecordRepository.findById(100L)).thenReturn(Optional.of(sampleRecord));

        PaymentRequest req = PaymentRequest.builder()
                .feeRecordId(100L)
                .paymentAmount(BigDecimal.valueOf(16000)) // Outstanding is 15000
                .paymentMethod("Online")
                .build();

        ConflictException ex = assertThrows(ConflictException.class, () -> feeService.recordPayment(req));
        assertTrue(ex.getMessage().contains("exceeds outstanding balance"));
        verify(feePaymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Recording payment should reject zero or negative amounts")
    void recordPayment_ZeroOrNegative_ShouldThrowConflict() {
        when(feeRecordRepository.findById(100L)).thenReturn(Optional.of(sampleRecord));

        PaymentRequest zeroReq = PaymentRequest.builder()
                .feeRecordId(100L)
                .paymentAmount(BigDecimal.ZERO)
                .build();
        assertThrows(ConflictException.class, () -> feeService.recordPayment(zeroReq));

        PaymentRequest negReq = PaymentRequest.builder()
                .feeRecordId(100L)
                .paymentAmount(BigDecimal.valueOf(-500))
                .build();
        assertThrows(ConflictException.class, () -> feeService.recordPayment(negReq));
    }

    @Test
    @DisplayName("Recording payment should reject fully paid records")
    void recordPayment_AlreadyFullyPaid_ShouldThrowConflict() {
        sampleRecord.setStatus("Paid");
        sampleRecord.setPaidAmount(BigDecimal.valueOf(25000));
        when(feeRecordRepository.findById(100L)).thenReturn(Optional.of(sampleRecord));

        PaymentRequest req = PaymentRequest.builder()
                .feeRecordId(100L)
                .paymentAmount(BigDecimal.valueOf(1000))
                .build();

        ConflictException ex = assertThrows(ConflictException.class, () -> feeService.recordPayment(req));
        assertTrue(ex.getMessage().contains("already fully paid"));
    }

    @Test
    @DisplayName("Recording payment with duplicate transaction reference should be rejected")
    void recordPayment_DuplicateReference_ShouldThrowConflict() {
        when(feeRecordRepository.findById(100L)).thenReturn(Optional.of(sampleRecord));
        when(feePaymentRepository.existsByTransactionReferenceAndFeeRecordId("UTR123456", 100L)).thenReturn(true);

        PaymentRequest req = PaymentRequest.builder()
                .feeRecordId(100L)
                .paymentAmount(BigDecimal.valueOf(5000))
                .transactionReference("UTR123456")
                .paymentMethod("Online")
                .build();

        ConflictException ex = assertThrows(ConflictException.class, () -> feeService.recordPayment(req));
        assertTrue(ex.getMessage().contains("already been recorded"));
    }

    // ── SUCCESSFUL PAYMENT & LEDGER CREATION ──────────────────

    @Test
    @DisplayName("Successful full payment creates FeePayment transaction and sets status to Paid")
    void recordPayment_FullPayment_Success() {
        when(feeRecordRepository.findById(100L)).thenReturn(Optional.of(sampleRecord));
        when(feePaymentRepository.countReceiptsByYear(anyInt())).thenReturn(5L);
        when(feePaymentRepository.existsByReceiptNumber(anyString())).thenReturn(false);

        when(feePaymentRepository.save(any(FeePayment.class))).thenAnswer(invocation -> {
            FeePayment p = invocation.getArgument(0);
            p.setId(999L);
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        when(feeRecordRepository.save(any(FeeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentRequest req = PaymentRequest.builder()
                .feeRecordId(100L)
                .paymentAmount(BigDecimal.valueOf(15000)) // Exact outstanding balance
                .paymentMethod("Online")
                .transactionReference("TXN998877")
                .remarks("Final semester payment")
                .recordedBy(1L)
                .build();

        PaymentResponse response = feeService.recordPayment(req);

        assertNotNull(response);
        assertEquals(999L, response.getPaymentId());
        assertEquals(BigDecimal.valueOf(15000), response.getAmountPaid());
        assertEquals(BigDecimal.valueOf(25000), response.getCumulativePaidAmount());
        assertEquals(BigDecimal.ZERO, response.getOutstandingBalance());
        assertEquals("Paid", response.getFeeRecordStatus());
        assertTrue(response.getReceiptNumber().startsWith("REC-"));

        verify(feePaymentRepository, times(1)).save(any(FeePayment.class));
        verify(feeRecordRepository, times(1)).save(sampleRecord);
        assertEquals("Paid", sampleRecord.getStatus());
        assertEquals(BigDecimal.valueOf(25000), sampleRecord.getPaidAmount());
    }

    // ── DELETE PROTECTION TEST ────────────────────────────────

    @Test
    @DisplayName("Deleting fee record with existing payment transactions must throw ConflictException")
    void deleteFeeRecord_WithPayments_ShouldThrowConflict() {
        when(feeRecordRepository.findById(100L)).thenReturn(Optional.of(sampleRecord));
        when(feePaymentRepository.countByFeeRecordId(100L)).thenReturn(2L);

        ConflictException ex = assertThrows(ConflictException.class, () -> feeService.deleteFeeRecord(100L));
        assertTrue(ex.getMessage().contains("Cannot delete fee record with existing payment transactions"));
        verify(feeRecordRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Deleting fee record with zero payments should succeed")
    void deleteFeeRecord_ZeroPayments_Success() {
        sampleRecord.setPaidAmount(BigDecimal.ZERO);
        sampleRecord.setStatus("Unpaid");
        when(feeRecordRepository.findById(100L)).thenReturn(Optional.of(sampleRecord));
        when(feePaymentRepository.countByFeeRecordId(100L)).thenReturn(0L);

        assertDoesNotThrow(() -> feeService.deleteFeeRecord(100L));
        verify(feeRecordRepository, times(1)).deleteById(100L);
    }

    // ── RECEIPT GENERATION TEST ───────────────────────────────

    @Test
    @DisplayName("Get receipt should assemble full student, record, and payment details")
    void getReceipt_Success() {
        FeePayment payment = FeePayment.builder()
                .id(55L)
                .feeRecordId(100L)
                .amountPaid(BigDecimal.valueOf(10000))
                .paymentMethod("Cash")
                .receiptNumber("REC-2026-00055")
                .paymentDate(LocalDate.of(2026, 9, 11))
                .remarks("Counter cash payment")
                .recordedBy(1L)
                .createdAt(LocalDateTime.now())
                .build();

        when(feePaymentRepository.findById(55L)).thenReturn(Optional.of(payment));
        when(feeRecordRepository.findById(100L)).thenReturn(Optional.of(sampleRecord));

        ReceiptResponse receipt = feeService.getReceipt(55L);

        assertNotNull(receipt);
        assertEquals("REC-2026-00055", receipt.getReceiptNumber());
        assertEquals("Aarav Mehta", receipt.getStudentName());
        assertEquals("10-A", receipt.getStudentClass());
        assertEquals(BigDecimal.valueOf(25000), receipt.getTotalFee());
        assertEquals(BigDecimal.valueOf(10000), receipt.getAmountPaidInThisTx());
        assertEquals(BigDecimal.valueOf(10000), receipt.getCumulativePaidAmount());
        assertEquals(BigDecimal.valueOf(15000), receipt.getOutstandingAmount());
    }

    // ── SECURITY USERCONTEXT TESTS ─────────────────────────────

    @Test
    @DisplayName("UserContext: Admin can access all student records")
    void userContext_AdminCanAccessAnyStudent() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "1");
        req.addHeader("X-User-Role", "ADMIN");

        assertDoesNotThrow(() -> UserContext.requireRole(req, "ADMIN"));
        assertDoesNotThrow(() -> UserContext.requireStudentOwnershipOrAdmin(req, 999L));
    }

    @Test
    @DisplayName("UserContext: Student can only access their own studentId")
    void userContext_StudentOwnershipEnforced() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "20");
        req.addHeader("X-User-Role", "STUDENT");
        req.addHeader("X-Student-Id", "50");

        // Own record (50) -> should pass
        assertDoesNotThrow(() -> UserContext.requireStudentOwnershipOrAdmin(req, 50L));

        // Other student record (51) -> should throw ForbiddenException
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> UserContext.requireStudentOwnershipOrAdmin(req, 51L));
        assertTrue(ex.getMessage().contains("not authorized"));
    }

    @Test
    @DisplayName("UserContext: Teacher or unauthenticated access to admin endpoints is rejected")
    void userContext_TeacherAccessRejected() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", "10");
        req.addHeader("X-User-Role", "TEACHER");

        assertThrows(ForbiddenException.class, () -> UserContext.requireRole(req, "ADMIN"));

        MockHttpServletRequest unauthReq = new MockHttpServletRequest();
        assertThrows(UnauthorizedException.class, () -> UserContext.requireRole(unauthReq, "ADMIN"));
    }
}
