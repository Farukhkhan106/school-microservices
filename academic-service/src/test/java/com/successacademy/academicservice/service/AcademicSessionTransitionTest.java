package com.successacademy.academicservice.service;

import com.successacademy.academicservice.client.FeeServiceClient;
import com.successacademy.academicservice.client.StudentServiceClient;
import com.successacademy.academicservice.dto.PromotionDecisionUpdateRequest;
import com.successacademy.academicservice.dto.SessionTransitionRequest;
import com.successacademy.academicservice.dto.StudentPromotionItemDto;
import com.successacademy.academicservice.dto.TransitionPreviewResponse;
import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.AcademicSessionRepository;
import com.successacademy.academicservice.repository.AcademicSessionTransitionRepository;
import com.successacademy.academicservice.repository.StudentEnrollmentRepository;
import com.successacademy.academicservice.repository.StudentPromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicSessionTransitionTest {

    @Mock
    private AcademicSessionRepository sessionRepository;

    @Mock
    private StudentEnrollmentRepository enrollmentRepository;

    @Mock
    private AcademicSessionTransitionRepository transitionRepository;

    @Mock
    private StudentPromotionRepository promotionRepository;

    @Mock
    private StudentServiceClient studentServiceClient;

    @Mock
    private FeeServiceClient feeServiceClient;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AcademicSessionTransitionService transitionService;

    private AcademicSession sourceSession;
    private AcademicSession targetSession;
    private StudentEnrollment rahul9A;
    private StudentEnrollment sara12A;

    @BeforeEach
    void setUp() {
        sourceSession = AcademicSession.builder()
                .id(1L)
                .tenantId("school_1")
                .sessionCode("2026-2027")
                .name("Academic Session 2026-27")
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2027, 3, 31))
                .status(SessionStatus.ACTIVE)
                .isActive(true)
                .build();

        targetSession = AcademicSession.builder()
                .id(2L)
                .tenantId("school_1")
                .sessionCode("2027-2028")
                .name("Academic Session 2027-28")
                .startDate(LocalDate.of(2027, 4, 1))
                .endDate(LocalDate.of(2028, 3, 31))
                .status(SessionStatus.UPCOMING)
                .isActive(false)
                .build();

        rahul9A = StudentEnrollment.builder()
                .id(101L)
                .tenantId("school_1")
                .sessionId(1L)
                .sessionCode("2026-2027")
                .studentId(501L)
                .studentName("Rahul Sharma")
                .admissionNo("ADM-501")
                .studentClass("9")
                .section("A")
                .rollNo("01")
                .status(EnrollmentStatus.ACTIVE)
                .build();

        sara12A = StudentEnrollment.builder()
                .id(102L)
                .tenantId("school_1")
                .sessionId(1L)
                .sessionCode("2026-2027")
                .studentId(502L)
                .studentName("Sara Khan")
                .admissionNo("ADM-502")
                .studentClass("12")
                .section("A")
                .rollNo("02")
                .status(EnrollmentStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Progression Rules - Class 9 promotes to Class 10, Class 12 graduates (No Class 13)")
    void testProgressionRules() {
        // Class 9 -> Class 10
        PromotionDecision d9 = transitionService.calculateSuggestedDecision("9");
        String next9 = transitionService.calculateNextClass("9", d9);
        assertThat(d9).isEqualTo(PromotionDecision.PROMOTE);
        assertThat(next9).isEqualTo("10");

        // Class 12 -> GRADUATE (Never Class 13)
        PromotionDecision d12 = transitionService.calculateSuggestedDecision("12");
        String next12 = transitionService.calculateNextClass("12", d12);
        assertThat(d12).isEqualTo(PromotionDecision.GRADUATE);
        assertThat(next12).isEqualTo("GRADUATED");

        // Retain -> stays in same class
        String retainClass = transitionService.calculateNextClass("9", PromotionDecision.RETAIN);
        assertThat(retainClass).isEqualTo("9");
    }

    @Test
    @DisplayName("Preview Transition - Calculates promotion candidates and fee carry-forward correctly")
    void testPreviewTransition_Success() {
        SessionTransitionRequest req = SessionTransitionRequest.builder()
                .sourceSessionId(1L)
                .targetSessionId(2L)
                .carryForwardFees(true)
                .notes("Annual transition")
                .build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(sourceSession));
        when(sessionRepository.findById(2L)).thenReturn(Optional.of(targetSession));
        when(enrollmentRepository.findByTenantIdAndSessionId("school_1", 1L)).thenReturn(List.of(rahul9A, sara12A));

        // Mock fee arrears for Rahul: ₹10,000 outstanding
        FeeServiceClient.StudentFeeSummaryDto feeDto = FeeServiceClient.StudentFeeSummaryDto.builder()
                .studentId(501L)
                .outstandingBalance(new BigDecimal("10000.00"))
                .build();
        when(feeServiceClient.getStudentFeeSummaries("2026-2027")).thenReturn(List.of(feeDto));

        when(transitionRepository.findByTenantIdAndSourceSessionIdAndTargetSessionId("school_1", 1L, 2L))
                .thenReturn(Optional.empty());
        when(transitionRepository.save(any(AcademicSessionTransition.class))).thenAnswer(invocation -> {
            AcademicSessionTransition t = invocation.getArgument(0);
            t.setId(10L);
            return t;
        });

        when(promotionRepository.findByTenantIdAndTransitionId("school_1", 10L)).thenReturn(new ArrayList<>());
        when(promotionRepository.save(any(StudentPromotion.class))).thenAnswer(invocation -> {
            StudentPromotion p = invocation.getArgument(0);
            p.setId(99L);
            return p;
        });

        TransitionPreviewResponse preview = transitionService.previewTransition(req, "school_1", 1L);

        assertThat(preview).isNotNull();
        assertThat(preview.getTotalStudents()).isEqualTo(2);
        assertThat(preview.getPromotedCount()).isEqualTo(1); // Rahul
        assertThat(preview.getGraduatedCount()).isEqualTo(1); // Sara
        assertThat(preview.getTotalCarryForwardFees()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(preview.getFeeArrearsStudentsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Update Promotion Decision - Admin can override system suggestion (e.g. Aman retained)")
    void testUpdatePromotionDecision_AdminOverride() {
        AcademicSessionTransition trans = AcademicSessionTransition.builder()
                .id(10L)
                .tenantId("school_1")
                .status(TransitionStatus.REVIEW)
                .build();

        StudentPromotion rahul = StudentPromotion.builder()
                .id(201L)
                .tenantId("school_1")
                .transitionId(10L)
                .studentId(501L)
                .sourceClass("9")
                .sourceSection("A")
                .targetClass("10")
                .targetSection("A")
                .suggestedDecision(PromotionDecision.PROMOTE)
                .finalDecision(PromotionDecision.PROMOTE)
                .build();

        when(transitionRepository.findById(10L)).thenReturn(Optional.of(trans));
        when(promotionRepository.findByTenantIdAndTransitionIdAndStudentId("school_1", 10L, 501L)).thenReturn(Optional.of(rahul));
        when(promotionRepository.save(any(StudentPromotion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(promotionRepository.findByTenantIdAndTransitionId("school_1", 10L)).thenReturn(List.of(rahul));

        PromotionDecisionUpdateRequest updateReq = PromotionDecisionUpdateRequest.builder()
                .finalDecision(PromotionDecision.RETAIN)
                .targetClass("9")
                .targetSection("A")
                .reason("Low attendance benchmark")
                .build();

        StudentPromotionItemDto updated = transitionService.updatePromotionDecision(10L, 501L, updateReq, "school_1", 1L);

        assertThat(updated.getFinalDecision()).isEqualTo(PromotionDecision.RETAIN);
        assertThat(updated.getReason()).isEqualTo("Low attendance benchmark");
        assertThat(updated.getTargetClass()).isEqualTo("9");
    }

    @Test
    @DisplayName("Execute Transition - Rollover creates target enrollments, updates source status, closes source session and activates target")
    void testExecuteTransition_SuccessfulRollover() {
        AcademicSessionTransition trans = AcademicSessionTransition.builder()
                .id(10L)
                .tenantId("school_1")
                .sourceSessionId(1L)
                .targetSessionId(2L)
                .status(TransitionStatus.READY)
                .build();

        StudentPromotion rahulPromo = StudentPromotion.builder()
                .id(201L)
                .tenantId("school_1")
                .transitionId(10L)
                .studentId(501L)
                .studentName("Rahul Sharma")
                .admissionNo("ADM-501")
                .sourceClass("9")
                .sourceSection("A")
                .targetClass("10")
                .targetSection("A")
                .finalDecision(PromotionDecision.PROMOTE)
                .build();

        StudentPromotion saraPromo = StudentPromotion.builder()
                .id(202L)
                .tenantId("school_1")
                .transitionId(10L)
                .studentId(502L)
                .studentName("Sara Khan")
                .admissionNo("ADM-502")
                .sourceClass("12")
                .sourceSection("A")
                .finalDecision(PromotionDecision.GRADUATE)
                .build();

        when(transitionRepository.findById(10L)).thenReturn(Optional.of(trans));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(sourceSession));
        when(sessionRepository.findById(2L)).thenReturn(Optional.of(targetSession));
        when(promotionRepository.findByTenantIdAndTransitionId("school_1", 10L)).thenReturn(List.of(rahulPromo, saraPromo));

        when(enrollmentRepository.findByTenantIdAndSessionIdAndStudentId("school_1", 2L, 501L)).thenReturn(Optional.empty());
        when(enrollmentRepository.findByTenantIdAndSessionIdAndStudentId("school_1", 1L, 501L)).thenReturn(Optional.of(rahul9A));
        when(enrollmentRepository.findByTenantIdAndSessionIdAndStudentId("school_1", 1L, 502L)).thenReturn(Optional.of(sara12A));
        when(transitionRepository.save(any(AcademicSessionTransition.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransitionPreviewResponse result = transitionService.executeTransition(10L, "school_1", 1L, "ADMIN");

        assertThat(result.getStatus()).isEqualTo(TransitionStatus.COMPLETED);

        // Source session closed
        assertThat(sourceSession.isActive()).isFalse();
        assertThat(sourceSession.getStatus()).isEqualTo(SessionStatus.CLOSED);

        // Target session active
        assertThat(targetSession.isActive()).isTrue();
        assertThat(targetSession.getStatus()).isEqualTo(SessionStatus.ACTIVE);

        // Rahul source marked PROMOTED
        assertThat(rahul9A.getStatus()).isEqualTo(EnrollmentStatus.PROMOTED);

        // Sara source marked GRADUATED
        assertThat(sara12A.getStatus()).isEqualTo(EnrollmentStatus.GRADUATED);

        // Target enrollment created for Rahul into Class 10
        verify(enrollmentRepository).save(argThat(e ->
                e.getStudentId().equals(501L) &&
                e.getSessionId().equals(2L) &&
                "10".equals(e.getStudentClass()) &&
                e.getStatus() == EnrollmentStatus.ACTIVE
        ));

        // NO target enrollment created for Sara (Graduated!)
        verify(enrollmentRepository, never()).save(argThat(e ->
                e.getStudentId().equals(502L) && e.getSessionId().equals(2L)
        ));
    }

    @Test
    @DisplayName("Idempotency - Re-running COMPLETED transition returns immediately without duplicating data")
    void testExecuteTransition_Idempotency() {
        AcademicSessionTransition completedTrans = AcademicSessionTransition.builder()
                .id(10L)
                .tenantId("school_1")
                .sourceSessionId(1L)
                .targetSessionId(2L)
                .status(TransitionStatus.COMPLETED)
                .build();

        when(transitionRepository.findById(10L)).thenReturn(Optional.of(completedTrans));
        when(promotionRepository.findByTenantIdAndTransitionId("school_1", 10L)).thenReturn(new ArrayList<>());

        TransitionPreviewResponse result = transitionService.executeTransition(10L, "school_1", 1L, "ADMIN");

        assertThat(result.getStatus()).isEqualTo(TransitionStatus.COMPLETED);

        // Verification: enrollmentRepository should NOT be saved again
        verify(enrollmentRepository, never()).save(any());
        verify(sessionRepository, never()).save(any());
    }
}
