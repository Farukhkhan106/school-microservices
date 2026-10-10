package com.successacademy.academicservice.service;

import com.successacademy.academicservice.client.FeeServiceClient;
import com.successacademy.academicservice.client.StudentServiceClient;
import com.successacademy.academicservice.dto.*;
import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.AcademicSessionRepository;
import com.successacademy.academicservice.repository.AcademicSessionTransitionRepository;
import com.successacademy.academicservice.repository.StudentEnrollmentRepository;
import com.successacademy.academicservice.repository.StudentPromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicSessionTransitionService {

    private final AcademicSessionRepository sessionRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AcademicSessionTransitionRepository transitionRepository;
    private final StudentPromotionRepository promotionRepository;
    private final StudentServiceClient studentServiceClient;
    private final FeeServiceClient feeServiceClient;
    private final AuditLogService auditLogService;

    @Transactional
    public TransitionPreviewResponse previewTransition(SessionTransitionRequest req, String tenantId, Long actorUserId) {
        String cleanTenant = resolveTenant(tenantId);

        if (req.getSourceSessionId().equals(req.getTargetSessionId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source and target academic sessions must be distinct.");
        }

        AcademicSession sourceSession = sessionRepository.findById(req.getSourceSessionId())
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source academic session not found."));

        AcademicSession targetSession = sessionRepository.findById(req.getTargetSessionId())
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target academic session not found."));

        if (sourceSession.getStatus() == SessionStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source session is ARCHIVED and cannot be transitioned.");
        }

        // 1. Ensure source enrollments exist (Bootstrap from student-service if needed)
        List<StudentEnrollment> sourceEnrollments = enrollmentRepository.findByTenantIdAndSessionId(cleanTenant, sourceSession.getId());
        if (sourceEnrollments.isEmpty()) {
            log.info("No enrollments found in source session {}. Bootstrapping from student service...", sourceSession.getSessionCode());
            var currentStudents = studentServiceClient.getAllStudents();
            for (var st : currentStudents) {
                if (st.getId() == null) continue;
                StudentEnrollment se = StudentEnrollment.builder()
                        .tenantId(cleanTenant)
                        .sessionId(sourceSession.getId())
                        .sessionCode(sourceSession.getSessionCode())
                        .studentId(st.getId())
                        .studentName((st.getFirstName() + " " + (st.getLastName() != null ? st.getLastName() : "")).trim())
                        .admissionNo(st.getAdmissionNo())
                        .studentClass(st.getStudentClass() != null ? st.getStudentClass().trim() : "1")
                        .section(st.getSection() != null ? st.getSection().trim() : "A")
                        .rollNo(st.getRollNo())
                        .status(EnrollmentStatus.ACTIVE)
                        .build();
                sourceEnrollments.add(enrollmentRepository.save(se));
            }
        }

        // 2. Fetch fee balances for source session
        Map<Long, BigDecimal> feeBalances = new HashMap<>();
        try {
            var feeSummaries = feeServiceClient.getStudentFeeSummaries(sourceSession.getSessionCode());
            for (var f : feeSummaries) {
                if (f.getStudentId() != null && f.getOutstandingBalance() != null) {
                    feeBalances.put(f.getStudentId(), f.getOutstandingBalance());
                }
            }
        } catch (Exception e) {
            log.warn("Could not retrieve fee balances: {}", e.getMessage());
        }

        // 3. Find or create transition record
        AcademicSessionTransition transition = transitionRepository
                .findByTenantIdAndSourceSessionIdAndTargetSessionId(cleanTenant, sourceSession.getId(), targetSession.getId())
                .orElseGet(() -> AcademicSessionTransition.builder()
                        .tenantId(cleanTenant)
                        .sourceSessionId(sourceSession.getId())
                        .sourceSessionCode(sourceSession.getSessionCode())
                        .targetSessionId(targetSession.getId())
                        .targetSessionCode(targetSession.getSessionCode())
                        .status(TransitionStatus.REVIEW)
                        .notes(req.getNotes())
                        .build());

        transition.setStatus(TransitionStatus.REVIEW);
        if (req.getNotes() != null) {
            transition.setNotes(req.getNotes());
        }
        AcademicSessionTransition savedTransition = transitionRepository.save(transition);

        // 4. Generate/Update promotion candidate items
        Map<Long, StudentPromotion> existingPromotions = promotionRepository
                .findByTenantIdAndTransitionId(cleanTenant, savedTransition.getId())
                .stream()
                .collect(Collectors.toMap(StudentPromotion::getStudentId, p -> p, (a, b) -> a));

        List<StudentPromotion> promotionList = new ArrayList<>();
        BigDecimal totalArrears = BigDecimal.ZERO;
        int feeArrearsCount = 0;

        for (StudentEnrollment se : sourceEnrollments) {
            StudentPromotion promo = existingPromotions.get(se.getStudentId());
            BigDecimal outstanding = feeBalances.getOrDefault(se.getStudentId(), BigDecimal.ZERO);
            BigDecimal carryForward = req.isCarryForwardFees() ? outstanding : BigDecimal.ZERO;

            if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
                totalArrears = totalArrears.add(outstanding);
                feeArrearsCount++;
            }

            if (promo == null) {
                PromotionDecision suggested = calculateSuggestedDecision(se.getStudentClass());
                String nextClass = calculateNextClass(se.getStudentClass(), suggested);

                promo = StudentPromotion.builder()
                        .tenantId(cleanTenant)
                        .transitionId(savedTransition.getId())
                        .studentId(se.getStudentId())
                        .studentName(se.getStudentName())
                        .admissionNo(se.getAdmissionNo())
                        .sourceSessionId(sourceSession.getId())
                        .targetSessionId(targetSession.getId())
                        .sourceClass(se.getStudentClass())
                        .sourceSection(se.getSection())
                        .targetClass(nextClass)
                        .targetSection(se.getSection())
                        .suggestedDecision(suggested)
                        .finalDecision(suggested)
                        .outstandingFee(outstanding)
                        .carryForwardFee(carryForward)
                        .status("PENDING")
                        .build();
            } else {
                promo.setOutstandingFee(outstanding);
                if (req.isCarryForwardFees()) {
                    promo.setCarryForwardFee(outstanding);
                }
            }
            promotionList.add(promotionRepository.save(promo));
        }

        // 5. Update transition counters
        int promoted = 0;
        int retained = 0;
        int graduated = 0;
        int transferred = 0;
        int withdrawn = 0;

        for (StudentPromotion p : promotionList) {
            switch (p.getFinalDecision()) {
                case PROMOTE -> promoted++;
                case RETAIN -> retained++;
                case GRADUATE -> graduated++;
                case TRANSFER -> transferred++;
                case WITHDRAW -> withdrawn++;
                default -> {}
            }
        }

        savedTransition.setTotalStudents(promotionList.size());
        savedTransition.setPromotedCount(promoted);
        savedTransition.setRetainedCount(retained);
        savedTransition.setGraduatedCount(graduated);
        savedTransition.setTransferredCount(transferred);
        savedTransition.setWithdrawnCount(withdrawn);
        savedTransition.setTotalCarryForwardFees(totalArrears);
        savedTransition.setFeeArrearsStudentsCount(feeArrearsCount);

        transitionRepository.save(savedTransition);

        auditLogService.log(actorUserId, "ADMIN", "TRANSITION_PREVIEW_GENERATED", "AcademicSessionTransition",
                savedTransition.getId(), null, savedTransition.getSourceSessionCode() + " -> " + savedTransition.getTargetSessionCode(),
                "Generated preview for " + savedTransition.getTotalStudents() + " students");

        return mapToTransitionResponse(savedTransition, promotionList);
    }

    public TransitionPreviewResponse getTransitionById(Long id, String tenantId) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSessionTransition t = transitionRepository.findById(id)
                .filter(tr -> cleanTenant.equalsIgnoreCase(tr.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transition not found with ID: " + id));

        List<StudentPromotion> promotions = promotionRepository.findByTenantIdAndTransitionId(cleanTenant, t.getId());
        return mapToTransitionResponse(t, promotions);
    }

    @Transactional
    public StudentPromotionItemDto updatePromotionDecision(
            Long transitionId,
            Long studentId,
            PromotionDecisionUpdateRequest req,
            String tenantId,
            Long actorUserId
    ) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSessionTransition transition = transitionRepository.findById(transitionId)
                .filter(t -> cleanTenant.equalsIgnoreCase(t.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transition not found"));

        if (transition.getStatus() == TransitionStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot alter decisions for an already COMPLETED transition.");
        }

        StudentPromotion promo = promotionRepository.findByTenantIdAndTransitionIdAndStudentId(cleanTenant, transitionId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student promotion record not found for student ID: " + studentId));

        promo.setFinalDecision(req.getFinalDecision());
        if (req.getTargetClass() != null) {
            promo.setTargetClass(req.getTargetClass().trim());
        }
        if (req.getTargetSection() != null) {
            promo.setTargetSection(req.getTargetSection().trim());
        }
        if (req.getCarryForwardFee() != null) {
            promo.setCarryForwardFee(req.getCarryForwardFee());
        }
        if (req.getReason() != null) {
            promo.setReason(req.getReason().trim());
        }
        promo.setReviewedBy(actorUserId);
        promo.setReviewedAt(LocalDateTime.now());
        promo.setStatus("APPROVED");

        StudentPromotion saved = promotionRepository.save(promo);

        // Recalculate transition summary counters
        recalculateTransitionCounts(transition, cleanTenant);

        return mapPromotionToDto(saved);
    }

    @Transactional
    public TransitionPreviewResponse executeTransition(Long transitionId, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSessionTransition transition = transitionRepository.findById(transitionId)
                .filter(t -> cleanTenant.equalsIgnoreCase(t.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transition not found with ID: " + transitionId));

        // Idempotency: If already completed, return without re-mutating
        if (transition.getStatus() == TransitionStatus.COMPLETED) {
            log.info("Transition {} already COMPLETED. Returning existing result idempotently.", transitionId);
            List<StudentPromotion> promotions = promotionRepository.findByTenantIdAndTransitionId(cleanTenant, transition.getId());
            return mapToTransitionResponse(transition, promotions);
        }

        AcademicSession sourceSession = sessionRepository.findById(transition.getSourceSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source session not found"));
        AcademicSession targetSession = sessionRepository.findById(transition.getTargetSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target session not found"));

        transition.setStatus(TransitionStatus.EXECUTING);
        transitionRepository.save(transition);

        try {
            List<StudentPromotion> candidates = promotionRepository.findByTenantIdAndTransitionId(cleanTenant, transition.getId());

            for (StudentPromotion cand : candidates) {
                PromotionDecision decision = cand.getFinalDecision();

                switch (decision) {
                    case PROMOTE -> {
                        // Create target enrollment (Idempotent upsert)
                        StudentEnrollment targetEnrollment = enrollmentRepository
                                .findByTenantIdAndSessionIdAndStudentId(cleanTenant, targetSession.getId(), cand.getStudentId())
                                .orElseGet(() -> StudentEnrollment.builder()
                                        .tenantId(cleanTenant)
                                        .sessionId(targetSession.getId())
                                        .sessionCode(targetSession.getSessionCode())
                                        .studentId(cand.getStudentId())
                                        .build());

                        targetEnrollment.setStudentName(cand.getStudentName());
                        targetEnrollment.setAdmissionNo(cand.getAdmissionNo());
                        targetEnrollment.setStudentClass(cand.getTargetClass() != null ? cand.getTargetClass().trim() : cand.getSourceClass());
                        targetEnrollment.setSection(cand.getTargetSection() != null ? cand.getTargetSection().trim() : cand.getSourceSection());
                        targetEnrollment.setStatus(EnrollmentStatus.ACTIVE);
                        enrollmentRepository.save(targetEnrollment);

                        // Mark source enrollment
                        enrollmentRepository.findByTenantIdAndSessionIdAndStudentId(cleanTenant, sourceSession.getId(), cand.getStudentId())
                                .ifPresent(se -> {
                                    se.setStatus(EnrollmentStatus.PROMOTED);
                                    enrollmentRepository.save(se);
                                });

                        // Sync student service placement
                        studentServiceClient.updateStudentPlacement(cand.getStudentId(), cand.getTargetClass(), cand.getTargetSection(), null, "Active");
                    }
                    case RETAIN -> {
                        StudentEnrollment targetEnrollment = enrollmentRepository
                                .findByTenantIdAndSessionIdAndStudentId(cleanTenant, targetSession.getId(), cand.getStudentId())
                                .orElseGet(() -> StudentEnrollment.builder()
                                        .tenantId(cleanTenant)
                                        .sessionId(targetSession.getId())
                                        .sessionCode(targetSession.getSessionCode())
                                        .studentId(cand.getStudentId())
                                        .build());

                        targetEnrollment.setStudentName(cand.getStudentName());
                        targetEnrollment.setAdmissionNo(cand.getAdmissionNo());
                        targetEnrollment.setStudentClass(cand.getSourceClass());
                        targetEnrollment.setSection(cand.getSourceSection());
                        targetEnrollment.setStatus(EnrollmentStatus.ACTIVE);
                        enrollmentRepository.save(targetEnrollment);

                        enrollmentRepository.findByTenantIdAndSessionIdAndStudentId(cleanTenant, sourceSession.getId(), cand.getStudentId())
                                .ifPresent(se -> {
                                    se.setStatus(EnrollmentStatus.RETAINED);
                                    enrollmentRepository.save(se);
                                });

                        studentServiceClient.updateStudentPlacement(cand.getStudentId(), cand.getSourceClass(), cand.getSourceSection(), null, "Active");
                    }
                    case GRADUATE -> {
                        enrollmentRepository.findByTenantIdAndSessionIdAndStudentId(cleanTenant, sourceSession.getId(), cand.getStudentId())
                                .ifPresent(se -> {
                                    se.setStatus(EnrollmentStatus.GRADUATED);
                                    enrollmentRepository.save(se);
                                });
                        studentServiceClient.updateStudentPlacement(cand.getStudentId(), cand.getSourceClass(), cand.getSourceSection(), null, "Graduated");
                    }
                    case TRANSFER -> {
                        enrollmentRepository.findByTenantIdAndSessionIdAndStudentId(cleanTenant, sourceSession.getId(), cand.getStudentId())
                                .ifPresent(se -> {
                                    se.setStatus(EnrollmentStatus.TRANSFERRED);
                                    enrollmentRepository.save(se);
                                });
                        studentServiceClient.updateStudentPlacement(cand.getStudentId(), cand.getSourceClass(), cand.getSourceSection(), null, "Transferred");
                    }
                    case WITHDRAW -> {
                        enrollmentRepository.findByTenantIdAndSessionIdAndStudentId(cleanTenant, sourceSession.getId(), cand.getStudentId())
                                .ifPresent(se -> {
                                    se.setStatus(EnrollmentStatus.WITHDRAWN);
                                    enrollmentRepository.save(se);
                                });
                        studentServiceClient.updateStudentPlacement(cand.getStudentId(), cand.getSourceClass(), cand.getSourceSection(), null, "Withdrawn");
                    }
                    default -> {}
                }

                cand.setStatus("EXECUTED");
                promotionRepository.save(cand);
            }

            // Close source session
            sourceSession.setActive(false);
            sourceSession.setStatus(SessionStatus.CLOSED);
            sourceSession.setUpdatedBy(actorUserId);
            sessionRepository.save(sourceSession);

            // Deactivate any other active session for this tenant
            sessionRepository.findByTenantIdAndIsActiveTrue(cleanTenant).ifPresent(s -> {
                if (!s.getId().equals(targetSession.getId())) {
                    s.setActive(false);
                    s.setStatus(SessionStatus.CLOSED);
                    sessionRepository.save(s);
                }
            });

            // Activate target session
            targetSession.setActive(true);
            targetSession.setStatus(SessionStatus.ACTIVE);
            targetSession.setUpdatedBy(actorUserId);
            sessionRepository.save(targetSession);

            transition.setStatus(TransitionStatus.COMPLETED);
            transition.setExecutedBy(actorUserId);
            transition.setExecutedAt(LocalDateTime.now());
            AcademicSessionTransition saved = transitionRepository.save(transition);

            auditLogService.log(actorUserId, actorRole, "ACADEMIC_YEAR_TRANSITION_COMPLETED", "AcademicSessionTransition",
                    saved.getId(), sourceSession.getSessionCode(), targetSession.getSessionCode(),
                    "Transition successfully finalized. Source session CLOSED, Target session ACTIVE.");

            return mapToTransitionResponse(saved, candidates);

        } catch (Exception e) {
            log.error("Session transition failed for transitionId={}: {}", transitionId, e.getMessage(), e);
            transition.setStatus(TransitionStatus.FAILED);
            transition.setNotes("Execution failure: " + e.getMessage());
            transitionRepository.save(transition);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Academic session transition execution failed: " + e.getMessage());
        }
    }

    // ── PROMOTION RULES ENGINE ───────────────────────────────────

    public PromotionDecision calculateSuggestedDecision(String currentClass) {
        if (currentClass == null) return PromotionDecision.PROMOTE;
        String clean = currentClass.trim().toUpperCase();
        if ("12".equals(clean) || "CLASS 12".equals(clean) || "XII".equals(clean)) {
            return PromotionDecision.GRADUATE;
        }
        return PromotionDecision.PROMOTE;
    }

    public String calculateNextClass(String currentClass, PromotionDecision decision) {
        if (decision == PromotionDecision.GRADUATE) {
            return "GRADUATED";
        }
        if (decision == PromotionDecision.RETAIN) {
            return currentClass;
        }
        if (currentClass == null || currentClass.isBlank()) {
            return "1";
        }

        String clean = currentClass.trim().toUpperCase();
        if ("NURSERY".equals(clean) || "NUR".equals(clean)) return "LKG";
        if ("LKG".equals(clean) || "KG-1".equals(clean)) return "UKG";
        if ("UKG".equals(clean) || "KG-2".equals(clean)) return "1";

        try {
            int num = Integer.parseInt(clean.replaceAll("[^0-9]", ""));
            if (num >= 1 && num < 12) {
                return String.valueOf(num + 1);
            } else if (num >= 12) {
                return "GRADUATED";
            }
        } catch (NumberFormatException ignored) {}

        return currentClass;
    }

    private void recalculateTransitionCounts(AcademicSessionTransition t, String tenantId) {
        List<StudentPromotion> list = promotionRepository.findByTenantIdAndTransitionId(tenantId, t.getId());
        int p = 0, r = 0, g = 0, tr = 0, w = 0;
        BigDecimal feeTotal = BigDecimal.ZERO;
        int arrearsCount = 0;

        for (StudentPromotion sp : list) {
            switch (sp.getFinalDecision()) {
                case PROMOTE -> p++;
                case RETAIN -> r++;
                case GRADUATE -> g++;
                case TRANSFER -> tr++;
                case WITHDRAW -> w++;
                default -> {}
            }
            if (sp.getCarryForwardFee() != null && sp.getCarryForwardFee().compareTo(BigDecimal.ZERO) > 0) {
                feeTotal = feeTotal.add(sp.getCarryForwardFee());
                arrearsCount++;
            }
        }

        t.setTotalStudents(list.size());
        t.setPromotedCount(p);
        t.setRetainedCount(r);
        t.setGraduatedCount(g);
        t.setTransferredCount(tr);
        t.setWithdrawnCount(w);
        t.setTotalCarryForwardFees(feeTotal);
        t.setFeeArrearsStudentsCount(arrearsCount);
        transitionRepository.save(t);
    }

    private String resolveTenant(String tenantId) {
        return (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
    }

    private TransitionPreviewResponse mapToTransitionResponse(AcademicSessionTransition t, List<StudentPromotion> promos) {
        return TransitionPreviewResponse.builder()
                .id(t.getId())
                .tenantId(t.getTenantId())
                .sourceSessionId(t.getSourceSessionId())
                .sourceSessionCode(t.getSourceSessionCode())
                .targetSessionId(t.getTargetSessionId())
                .targetSessionCode(t.getTargetSessionCode())
                .status(t.getStatus())
                .totalStudents(t.getTotalStudents())
                .promotedCount(t.getPromotedCount())
                .retainedCount(t.getRetainedCount())
                .graduatedCount(t.getGraduatedCount())
                .transferredCount(t.getTransferredCount())
                .withdrawnCount(t.getWithdrawnCount())
                .totalCarryForwardFees(t.getTotalCarryForwardFees())
                .feeArrearsStudentsCount(t.getFeeArrearsStudentsCount())
                .students(promos != null ? promos.stream().map(this::mapPromotionToDto).toList() : Collections.emptyList())
                .notes(t.getNotes())
                .executedBy(t.getExecutedBy())
                .executedAt(t.getExecutedAt())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private StudentPromotionItemDto mapPromotionToDto(StudentPromotion sp) {
        return StudentPromotionItemDto.builder()
                .id(sp.getId())
                .studentId(sp.getStudentId())
                .studentName(sp.getStudentName())
                .admissionNo(sp.getAdmissionNo())
                .sourceClass(sp.getSourceClass())
                .sourceSection(sp.getSourceSection())
                .targetClass(sp.getTargetClass())
                .targetSection(sp.getTargetSection())
                .suggestedDecision(sp.getSuggestedDecision())
                .finalDecision(sp.getFinalDecision())
                .outstandingFee(sp.getOutstandingFee())
                .carryForwardFee(sp.getCarryForwardFee())
                .status(sp.getStatus())
                .reason(sp.getReason())
                .reviewedBy(sp.getReviewedBy())
                .reviewedAt(sp.getReviewedAt())
                .build();
    }
}
