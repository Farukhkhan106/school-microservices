package com.successacademy.feeservice.service;

import com.successacademy.feeservice.dto.FeePlanDTOs.*;
import com.successacademy.feeservice.dto.FeeStatsResponse;
import com.successacademy.feeservice.dto.PaymentRequest;
import com.successacademy.feeservice.dto.PaymentResponse;
import com.successacademy.feeservice.dto.ReceiptResponse;
import com.successacademy.feeservice.exception.ConflictException;
import com.successacademy.feeservice.exception.NotFoundException;
import com.successacademy.feeservice.model.*;
import com.successacademy.feeservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeServiceImpl implements FeeService {

    private final FeeRecordRepository feeRecordRepository;
    private final FeeStructureRepository feeStructureRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final StudentFeePlanRepository studentFeePlanRepository;
    private final FeeStructureItemRepository feeStructureItemRepository;
    private final FeePlanItemRepository feePlanItemRepository;
    private final FeeAdjustmentRepository feeAdjustmentRepository;
    private final FeePlanAuditRepository feePlanAuditRepository;

    private static final String DEFAULT_ACADEMIC_YEAR = "2026-27";

    // ── FEE STRUCTURE ──────────────────────────────────────────

    @Override
    public List<FeeStructure> getAllFeeStructures() {
        return feeStructureRepository.findAll();
    }

    @Override
    public FeeStructure getFeeStructureByClass(String className) {
        return findStructureForClass(className)
                .orElseThrow(() -> new NotFoundException("Fee structure not found for class: " + className));
    }

    private Optional<FeeStructure> findStructureForClass(String className) {
        if (className == null || className.isBlank()) return Optional.empty();
        String norm = className.trim().toLowerCase();

        // 1. Direct match
        Optional<FeeStructure> direct = feeStructureRepository.findByClassNameIgnoreCase(className.trim());
        if (direct.isPresent()) return direct;

        // 2. Contains match among all structures
        List<FeeStructure> all = feeStructureRepository.findAll();
        for (FeeStructure s : all) {
            String sName = s.getClassName().toLowerCase();
            if (sName.equalsIgnoreCase(norm) || sName.contains(norm) || norm.contains(sName)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    @Override
    public FeeStructure addFeeStructure(FeeStructure structure) {
        calculateStructureTotal(structure);
        return feeStructureRepository.save(structure);
    }

    @Override
    public FeeStructure updateFeeStructure(Long id, FeeStructure structure) {
        FeeStructure existing = feeStructureRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fee structure not found with id: " + id));
        existing.setClassName(structure.getClassName());
        existing.setTuitionFee(structure.getTuitionFee());
        existing.setTransportFee(structure.getTransportFee());
        existing.setLibraryFee(structure.getLibraryFee());
        existing.setLabFee(structure.getLabFee());
        existing.setSportsFee(structure.getSportsFee());
        existing.setBooksFee(structure.getBooksFee());
        existing.setUniformFee(structure.getUniformFee());
        existing.setExamFee(structure.getExamFee());
        existing.setOtherFee(structure.getOtherFee());
        calculateStructureTotal(existing);
        return feeStructureRepository.save(existing);
    }

    private void calculateStructureTotal(FeeStructure s) {
        BigDecimal total = BigDecimal.ZERO;
        if (s.getTuitionFee() != null) total = total.add(s.getTuitionFee());
        if (s.getTransportFee() != null) total = total.add(s.getTransportFee());
        if (s.getLibraryFee() != null) total = total.add(s.getLibraryFee());
        if (s.getLabFee() != null) total = total.add(s.getLabFee());
        if (s.getSportsFee() != null) total = total.add(s.getSportsFee());
        if (s.getBooksFee() != null) total = total.add(s.getBooksFee());
        if (s.getUniformFee() != null) total = total.add(s.getUniformFee());
        if (s.getExamFee() != null) total = total.add(s.getExamFee());
        if (s.getOtherFee() != null) total = total.add(s.getOtherFee());
        s.setTotalFee(total);
    }

    @Override
    public void deleteFeeStructure(Long id) {
        if (!feeStructureRepository.existsById(id)) {
            throw new NotFoundException("Fee structure not found with id: " + id);
        }
        feeStructureRepository.deleteById(id);
    }

    // ── FEE RECORDS ─────────────────────────────────────────────

    @Override
    public List<FeeRecord> getAllFeeRecords() {
        return feeRecordRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .toList();
    }

    @Override
    public FeeRecord getFeeRecordById(Long id) {
        return feeRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fee record not found with id: " + id));
    }

    @Override
    public List<FeeRecord> getFeeRecordsByStudent(Long studentId) {
        return feeRecordRepository.findByStudentId(studentId);
    }

    @Override
    public List<FeeRecord> getFeeRecordsByStatus(String status) {
        return feeRecordRepository.findByStatus(status);
    }

    @Override
    public FeeRecord addFeeRecord(FeeRecord record) {
        if (record.getStatus() == null || record.getStatus().isBlank()) {
            record.setStatus("Unpaid");
        }
        if (record.getPaidAmount() == null) {
            record.setPaidAmount(BigDecimal.ZERO);
        }
        if (record.getAmount() == null || record.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException("Fee amount cannot be negative");
        }
        return feeRecordRepository.save(record);
    }

    // ── PAYMENT LEDGER IMPLEMENTATION ────────────────────────────

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentResponse recordPayment(PaymentRequest request) {
        if (request == null || request.getFeeRecordId() == null) {
            throw new ConflictException("Fee record ID is required");
        }

        FeeRecord record = feeRecordRepository.findById(request.getFeeRecordId())
                .orElseThrow(() -> new NotFoundException("Fee record not found with id: " + request.getFeeRecordId()));

        // 1. Validate payment amount
        BigDecimal paymentAmount = request.getPaymentAmount();
        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException("Payment amount must be greater than zero");
        }

        // 2. Validate current record status & outstanding balance
        if ("Paid".equalsIgnoreCase(record.getStatus())) {
            throw new ConflictException("Fee record is already fully paid");
        }

        BigDecimal currentPaid = record.getPaidAmount() != null ? record.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = record.getAmount() != null ? record.getAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = totalAmount.subtract(currentPaid);

        if (paymentAmount.compareTo(outstanding) > 0) {
            throw new ConflictException(
                    String.format("Payment amount (₹%s) exceeds outstanding balance (₹%s)",
                            paymentAmount.toPlainString(), outstanding.toPlainString())
            );
        }

        // 3. Duplicate transaction reference check (if reference is provided)
        String ref = request.getTransactionReference();
        if (ref != null && !ref.isBlank()) {
            ref = ref.trim();
            if (feePaymentRepository.existsByTransactionReferenceAndFeeRecordId(ref, record.getId())) {
                throw new ConflictException("A payment with reference '" + ref + "' has already been recorded for this fee record.");
            }
        } else {
            ref = null;
        }

        // 4. Generate unique immutable receipt number
        String receiptNumber = generateReceiptNumber();

        // 5. Persist immutable Payment Transaction
        LocalDate paymentDate = request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now();
        String method = request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()
                ? request.getPaymentMethod().trim() : "Cash";

        FeePayment payment = FeePayment.builder()
                .feeRecordId(record.getId())
                .amountPaid(paymentAmount)
                .paymentMethod(method)
                .transactionReference(ref)
                .receiptNumber(receiptNumber)
                .paymentDate(paymentDate)
                .remarks(request.getRemarks() != null ? request.getRemarks().trim() : null)
                .recordedBy(request.getRecordedBy())
                .build();

        FeePayment savedPayment = feePaymentRepository.save(payment);

        // 6. Recalculate and transactionally update aggregate on FeeRecord
        BigDecimal newPaid = feePaymentRepository.sumPaidByFeeRecordId(record.getId());
        record.setPaidAmount(newPaid);
        record.setPaymentDate(paymentDate);
        record.setPaymentMethod(method);
        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            record.setRemarks(request.getRemarks().trim());
        }

        // Status update
        if (newPaid.compareTo(totalAmount) >= 0 && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            record.setStatus("Paid");
        } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
            record.setStatus("Partial");
        } else {
            record.setStatus("Unpaid");
        }

        FeeRecord updatedRecord = feeRecordRepository.save(record);

        log.info("Recorded payment #{} of ₹{} for FeeRecord #{}, Receipt: {}",
                savedPayment.getId(), paymentAmount, record.getId(), receiptNumber);

        BigDecimal remainingBalance = totalAmount.subtract(newPaid);

        return PaymentResponse.builder()
                .paymentId(savedPayment.getId())
                .feeRecordId(record.getId())
                .amountPaid(savedPayment.getAmountPaid())
                .paymentMethod(savedPayment.getPaymentMethod())
                .transactionReference(savedPayment.getTransactionReference())
                .receiptNumber(savedPayment.getReceiptNumber())
                .paymentDate(savedPayment.getPaymentDate())
                .remarks(savedPayment.getRemarks())
                .recordedBy(savedPayment.getRecordedBy())
                .createdAt(savedPayment.getCreatedAt())
                .totalAmount(totalAmount)
                .cumulativePaidAmount(newPaid)
                .outstandingBalance(remainingBalance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remainingBalance)
                .feeRecordStatus(updatedRecord.getStatus())
                .build();
    }

    @Override
    public void deleteFeeRecord(Long id) {
        FeeRecord record = feeRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fee record not found with id: " + id));

        // Delete Protection: reject deletion if payments exist or paidAmount > 0
        long paymentCount = feePaymentRepository.countByFeeRecordId(id);
        if (paymentCount > 0 || (record.getPaidAmount() != null && record.getPaidAmount().compareTo(BigDecimal.ZERO) > 0)) {
            throw new ConflictException(
                    "Cannot delete fee record with existing payment transactions (" + paymentCount + " payment(s) recorded). Financial records must be preserved."
            );
        }

        feeRecordRepository.deleteById(id);
    }

    // ── PAYMENT HISTORY & RECEIPT METHODS ────────────────────────

    @Override
    public List<PaymentResponse> getPaymentHistory(Long feeRecordId) {
        FeeRecord record = feeRecordRepository.findById(feeRecordId)
                .orElseThrow(() -> new NotFoundException("Fee record not found with id: " + feeRecordId));

        BigDecimal total = record.getAmount() != null ? record.getAmount() : BigDecimal.ZERO;
        BigDecimal paid = record.getPaidAmount() != null ? record.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = total.subtract(paid);

        return feePaymentRepository.findByFeeRecordIdOrderByCreatedAtDesc(feeRecordId)
                .stream()
                .map(p -> PaymentResponse.builder()
                        .paymentId(p.getId())
                        .feeRecordId(p.getFeeRecordId())
                        .amountPaid(p.getAmountPaid())
                        .paymentMethod(p.getPaymentMethod())
                        .transactionReference(p.getTransactionReference())
                        .receiptNumber(p.getReceiptNumber())
                        .paymentDate(p.getPaymentDate())
                        .remarks(p.getRemarks())
                        .recordedBy(p.getRecordedBy())
                        .createdAt(p.getCreatedAt())
                        .totalAmount(total)
                        .cumulativePaidAmount(paid)
                        .outstandingBalance(outstanding.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : outstanding)
                        .feeRecordStatus(record.getStatus())
                        .build())
                .toList();
    }

    @Override
    public ReceiptResponse getReceipt(Long paymentId) {
        FeePayment payment = feePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment transaction not found with id: " + paymentId));

        FeeRecord record = feeRecordRepository.findById(payment.getFeeRecordId())
                .orElseThrow(() -> new NotFoundException("Fee record not found with id: " + payment.getFeeRecordId()));

        BigDecimal total = record.getAmount() != null ? record.getAmount() : BigDecimal.ZERO;
        BigDecimal cumulativePaid = record.getPaidAmount() != null ? record.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = total.subtract(cumulativePaid);

        return ReceiptResponse.builder()
                .receiptNumber(payment.getReceiptNumber())
                .paymentId(payment.getId())
                .feeRecordId(record.getId())
                .studentId(record.getStudentId())
                .studentName(record.getStudentName())
                .studentClass(record.getStudentClass())
                .feeType(record.getFeeType())
                .totalFee(total)
                .amountPaidInThisTx(payment.getAmountPaid())
                .cumulativePaidAmount(cumulativePaid)
                .outstandingAmount(outstanding.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : outstanding)
                .paymentMethod(payment.getPaymentMethod())
                .transactionReference(payment.getTransactionReference())
                .paymentDate(payment.getPaymentDate())
                .remarks(payment.getRemarks())
                .recordedBy(payment.getRecordedBy())
                .status(record.getStatus())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public ReceiptResponse getReceiptByNumber(String receiptNumber) {
        FeePayment payment = feePaymentRepository.findByReceiptNumber(receiptNumber)
                .orElseThrow(() -> new NotFoundException("Receipt not found with number: " + receiptNumber));
        return getReceipt(payment.getId());
    }

    // ── STUDENT FEE PLANS (CUSTOMIZED BILLING) ───────────────────

    @Override
    public ClassFeeTemplateResponse getClassFeeTemplate(String studentClass, String academicYear) {
        String year = (academicYear != null && !academicYear.isBlank()) ? academicYear.trim() : DEFAULT_ACADEMIC_YEAR;
        Optional<FeeStructure> opt = findStructureForClass(studentClass);

        List<FeePlanItemDTO> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        if (opt.isPresent()) {
            FeeStructure s = opt.get();
            // 1. Check if explicit FeeStructureItems exist
            List<FeeStructureItem> dbItems = feeStructureItemRepository.findByFeeStructureIdAndActiveTrue(s.getId());
            if (!dbItems.isEmpty()) {
                for (FeeStructureItem it : dbItems) {
                    items.add(FeePlanItemDTO.builder()
                            .feeHead(it.getFeeHead())
                            .standardAmount(it.getAmount())
                            .applicableAmount(it.getAmount())
                            .mandatory(it.getMandatory())
                            .enabled(true)
                            .build());
                    total = total.add(it.getAmount());
                }
            } else {
                // 2. Build template from FeeStructure standard columns
                addTemplateItem(items, "Tuition", s.getTuitionFee() != null ? s.getTuitionFee() : new BigDecimal("25000"), true);
                addTemplateItem(items, "Lab", s.getLabFee() != null ? s.getLabFee() : new BigDecimal("3000"), true);
                addTemplateItem(items, "Library", s.getLibraryFee() != null ? s.getLibraryFee() : new BigDecimal("2000"), true);
                addTemplateItem(items, "Transport", s.getTransportFee() != null ? s.getTransportFee() : new BigDecimal("8000"), false);
                addTemplateItem(items, "Books", s.getBooksFee() != null ? s.getBooksFee() : new BigDecimal("2000"), false);
                addTemplateItem(items, "Uniform", s.getUniformFee() != null ? s.getUniformFee() : new BigDecimal("2000"), false);
                if (s.getSportsFee() != null && s.getSportsFee().compareTo(BigDecimal.ZERO) > 0) {
                    addTemplateItem(items, "Sports", s.getSportsFee(), false);
                }
                if (s.getExamFee() != null && s.getExamFee().compareTo(BigDecimal.ZERO) > 0) {
                    addTemplateItem(items, "Exam", s.getExamFee(), false);
                }
                for (FeePlanItemDTO it : items) {
                    total = total.add(it.getStandardAmount());
                }
            }
        } else {
            // Default template for fallback
            addTemplateItem(items, "Tuition", new BigDecimal("25000"), true);
            addTemplateItem(items, "Lab", new BigDecimal("3000"), true);
            addTemplateItem(items, "Library", new BigDecimal("2000"), true);
            addTemplateItem(items, "Transport", new BigDecimal("8000"), false);
            addTemplateItem(items, "Books", new BigDecimal("2000"), false);
            addTemplateItem(items, "Uniform", new BigDecimal("2000"), false);
            total = new BigDecimal("42000");
        }

        return ClassFeeTemplateResponse.builder()
                .className(studentClass)
                .academicYear(year)
                .standardTotal(total)
                .items(items)
                .build();
    }

    private void addTemplateItem(List<FeePlanItemDTO> list, String head, BigDecimal amount, boolean mandatory) {
        list.add(FeePlanItemDTO.builder()
                .feeHead(head)
                .standardAmount(amount)
                .applicableAmount(amount)
                .mandatory(mandatory)
                .enabled(true)
                .build());
    }

    @Override
    public StudentFeePlanResponse getStudentFeePlan(Long studentId, String academicYear) {
        String year = (academicYear != null && !academicYear.isBlank()) ? academicYear.trim() : DEFAULT_ACADEMIC_YEAR;
        Optional<StudentFeePlan> opt = studentFeePlanRepository.findByStudentIdAndAcademicYear(studentId, year);

        if (opt.isEmpty()) {
            // Return SETUP_REQUIRED response without creating false debt
            return StudentFeePlanResponse.builder()
                    .studentId(studentId)
                    .academicYear(year)
                    .feePlanStatus("SETUP_REQUIRED")
                    .paymentStatus("UNPAID")
                    .standardFeeAmount(BigDecimal.ZERO)
                    .applicableAmount(BigDecimal.ZERO)
                    .totalAdjustmentAmount(BigDecimal.ZERO)
                    .finalPayableAmount(BigDecimal.ZERO)
                    .paidAmount(BigDecimal.ZERO)
                    .outstandingBalance(BigDecimal.ZERO)
                    .items(Collections.emptyList())
                    .adjustments(Collections.emptyList())
                    .audits(Collections.emptyList())
                    .build();
        }

        StudentFeePlan plan = opt.get();
        return buildPlanResponse(plan);
    }

    @Override
    @Transactional
    public StudentFeePlanResponse saveStudentFeePlan(StudentFeePlanRequest request, Long userId) {
        if (request == null || request.getStudentId() == null) {
            throw new ConflictException("Student ID is required");
        }

        String year = (request.getAcademicYear() != null && !request.getAcademicYear().isBlank())
                ? request.getAcademicYear().trim() : DEFAULT_ACADEMIC_YEAR;

        // Check if plan already exists for this academic year
        Optional<StudentFeePlan> existingOpt = studentFeePlanRepository.findByStudentIdAndAcademicYear(request.getStudentId(), year);
        if (existingOpt.isPresent()) {
            return updateStudentFeePlan(existingOpt.get().getId(), request, userId);
        }

        // Check if Admin intentionally chose SETUP_REQUIRED (Setup Later)
        boolean isSetupLater = "SETUP_REQUIRED".equalsIgnoreCase(request.getFeePlanStatus());

        BigDecimal standardTotal = BigDecimal.ZERO;
        BigDecimal applicableTotal = BigDecimal.ZERO;
        BigDecimal totalAdjustments = BigDecimal.ZERO;

        List<FeePlanItem> planItemsToSave = new ArrayList<>();
        List<FeeAdjustment> adjustmentsToSave = new ArrayList<>();

        if (!isSetupLater && request.getItems() != null) {
            for (FeePlanItemDTO itemDTO : request.getItems()) {
                BigDecimal stdAmt = itemDTO.getStandardAmount() != null ? itemDTO.getStandardAmount() : BigDecimal.ZERO;
                boolean enabled = itemDTO.getEnabled() != null && itemDTO.getEnabled();
                BigDecimal appAmt = enabled ? stdAmt : BigDecimal.ZERO;

                standardTotal = standardTotal.add(stdAmt);
                applicableTotal = applicableTotal.add(appAmt);
            }

            if (request.getAdjustments() != null) {
                for (FeeAdjustmentDTO adjDTO : request.getAdjustments()) {
                    BigDecimal adjAmt = adjDTO.getAmount() != null ? adjDTO.getAmount() : BigDecimal.ZERO;
                    if (adjAmt.compareTo(BigDecimal.ZERO) < 0) {
                        throw new ConflictException("Adjustment amount cannot be negative");
                    }
                    totalAdjustments = totalAdjustments.add(adjAmt);
                }
            }
        }

        BigDecimal finalPayable = applicableTotal.subtract(totalAdjustments);
        if (finalPayable.compareTo(BigDecimal.ZERO) < 0) {
            finalPayable = BigDecimal.ZERO;
        }

        String planStatus = isSetupLater ? "SETUP_REQUIRED" : "ACTIVE";

        StudentFeePlan plan = StudentFeePlan.builder()
                .studentId(request.getStudentId())
                .studentName(request.getStudentName())
                .studentClass(request.getStudentClass())
                .section(request.getSection())
                .academicYear(year)
                .standardFeeAmount(standardTotal)
                .applicableAmount(applicableTotal)
                .totalAdjustmentAmount(totalAdjustments)
                .finalPayableAmount(finalPayable)
                .feePlanStatus(planStatus)
                .dueDate(request.getDueDate() != null ? request.getDueDate() : LocalDate.now().plusMonths(1))
                .remarks(request.getRemarks())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        StudentFeePlan savedPlan = studentFeePlanRepository.save(plan);

        // Save child items
        if (!isSetupLater && request.getItems() != null) {
            for (FeePlanItemDTO itemDTO : request.getItems()) {
                BigDecimal stdAmt = itemDTO.getStandardAmount() != null ? itemDTO.getStandardAmount() : BigDecimal.ZERO;
                boolean enabled = itemDTO.getEnabled() != null && itemDTO.getEnabled();
                FeePlanItem item = FeePlanItem.builder()
                        .feePlanId(savedPlan.getId())
                        .feeHead(itemDTO.getFeeHead() != null ? itemDTO.getFeeHead() : "General Fee")
                        .standardAmount(stdAmt)
                        .applicableAmount(enabled ? stdAmt : BigDecimal.ZERO)
                        .mandatory(itemDTO.getMandatory() != null ? itemDTO.getMandatory() : false)
                        .enabled(enabled)
                        .build();
                feePlanItemRepository.save(item);
            }

            if (request.getAdjustments() != null) {
                for (FeeAdjustmentDTO adjDTO : request.getAdjustments()) {
                    FeeAdjustment adj = FeeAdjustment.builder()
                            .feePlanId(savedPlan.getId())
                            .studentId(savedPlan.getStudentId())
                            .adjustmentType(adjDTO.getAdjustmentType() != null ? adjDTO.getAdjustmentType() : "Discount")
                            .amount(adjDTO.getAmount() != null ? adjDTO.getAmount() : BigDecimal.ZERO)
                            .reason(adjDTO.getReason() != null ? adjDTO.getReason() : "Management concession")
                            .createdBy(userId)
                            .build();
                    feeAdjustmentRepository.save(adj);
                }
            }
        }

        // Synchronize with FeeRecord (Financial Account Receivable)
        if (!isSetupLater) {
            FeeRecord feeRecord = feeRecordRepository.findByStudentId(savedPlan.getStudentId())
                    .stream()
                    .filter(r -> (r.getAcademicYear() != null && r.getAcademicYear().equalsIgnoreCase(year))
                            || (r.getFeePlanId() != null && r.getFeePlanId().equals(savedPlan.getId())))
                    .findFirst()
                    .orElseGet(() -> FeeRecord.builder()
                            .studentId(savedPlan.getStudentId())
                            .studentName(savedPlan.getStudentName())
                            .studentClass(savedPlan.getStudentClass() + (savedPlan.getSection() != null ? "-" + savedPlan.getSection() : ""))
                            .feeType("Academic Fee Plan (" + year + ")")
                            .paidAmount(BigDecimal.ZERO)
                            .status("Unpaid")
                            .build());

            feeRecord.setFeePlanId(savedPlan.getId());
            feeRecord.setAcademicYear(year);
            feeRecord.setAmount(finalPayable);
            feeRecord.setDueDate(savedPlan.getDueDate());
            feeRecord.setRemarks(savedPlan.getRemarks());

            // Recalculate status based on actual payments
            BigDecimal paid = feePaymentRepository.sumPaidByFeeRecordId(feeRecord.getId() != null ? feeRecord.getId() : -1L);
            feeRecord.setPaidAmount(paid);
            if (paid.compareTo(finalPayable) >= 0 && finalPayable.compareTo(BigDecimal.ZERO) > 0) {
                feeRecord.setStatus("Paid");
            } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
                feeRecord.setStatus("Partial");
            } else {
                feeRecord.setStatus("Unpaid");
            }

            FeeRecord savedRecord = feeRecordRepository.save(feeRecord);
            savedPlan.setFeeRecordId(savedRecord.getId());
            studentFeePlanRepository.save(savedPlan);
        }

        log.info("Saved StudentFeePlan #{} for student #{} ({}) — Final Payable: ₹{}",
                savedPlan.getId(), savedPlan.getStudentId(), savedPlan.getStudentName(), finalPayable);

        return buildPlanResponse(savedPlan);
    }

    @Override
    @Transactional
    public StudentFeePlanResponse updateStudentFeePlan(Long planId, StudentFeePlanRequest request, Long userId) {
        StudentFeePlan existing = studentFeePlanRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Student fee plan not found with id: " + planId));

        BigDecimal previousPayable = existing.getFinalPayableAmount() != null ? existing.getFinalPayableAmount() : BigDecimal.ZERO;

        BigDecimal standardTotal = BigDecimal.ZERO;
        BigDecimal applicableTotal = BigDecimal.ZERO;
        BigDecimal totalAdjustments = BigDecimal.ZERO;

        if (request.getItems() != null) {
            for (FeePlanItemDTO itemDTO : request.getItems()) {
                BigDecimal stdAmt = itemDTO.getStandardAmount() != null ? itemDTO.getStandardAmount() : BigDecimal.ZERO;
                boolean enabled = itemDTO.getEnabled() != null && itemDTO.getEnabled();
                BigDecimal appAmt = enabled ? stdAmt : BigDecimal.ZERO;

                standardTotal = standardTotal.add(stdAmt);
                applicableTotal = applicableTotal.add(appAmt);
            }
        }

        if (request.getAdjustments() != null) {
            for (FeeAdjustmentDTO adjDTO : request.getAdjustments()) {
                BigDecimal adjAmt = adjDTO.getAmount() != null ? adjDTO.getAmount() : BigDecimal.ZERO;
                if (adjAmt.compareTo(BigDecimal.ZERO) < 0) {
                    throw new ConflictException("Adjustment amount cannot be negative");
                }
                totalAdjustments = totalAdjustments.add(adjAmt);
            }
        }

        BigDecimal newFinalPayable = applicableTotal.subtract(totalAdjustments);
        if (newFinalPayable.compareTo(BigDecimal.ZERO) < 0) {
            newFinalPayable = BigDecimal.ZERO;
        }

        // Audit check: if amount changed, record immutable FeePlanAudit entry
        if (previousPayable.compareTo(newFinalPayable) != 0) {
            String changeReason = (request.getChangeReason() != null && !request.getChangeReason().isBlank())
                    ? request.getChangeReason().trim() : "Plan updated by Administrator";

            FeePlanAudit audit = FeePlanAudit.builder()
                    .feePlanId(existing.getId())
                    .previousAmount(previousPayable)
                    .newAmount(newFinalPayable)
                    .changeReason(changeReason)
                    .changedBy(userId)
                    .build();
            feePlanAuditRepository.save(audit);
        }

        // Update plan fields
        existing.setStandardFeeAmount(standardTotal);
        existing.setApplicableAmount(applicableTotal);
        existing.setTotalAdjustmentAmount(totalAdjustments);
        existing.setFinalPayableAmount(newFinalPayable);
        existing.setFeePlanStatus("ACTIVE");
        if (request.getDueDate() != null) existing.setDueDate(request.getDueDate());
        if (request.getRemarks() != null) existing.setRemarks(request.getRemarks());
        existing.setUpdatedBy(userId);

        StudentFeePlan savedPlan = studentFeePlanRepository.save(existing);

        // Replace child items & adjustments
        feePlanItemRepository.deleteByFeePlanId(savedPlan.getId());
        if (request.getItems() != null) {
            for (FeePlanItemDTO itemDTO : request.getItems()) {
                BigDecimal stdAmt = itemDTO.getStandardAmount() != null ? itemDTO.getStandardAmount() : BigDecimal.ZERO;
                boolean enabled = itemDTO.getEnabled() != null && itemDTO.getEnabled();
                FeePlanItem item = FeePlanItem.builder()
                        .feePlanId(savedPlan.getId())
                        .feeHead(itemDTO.getFeeHead() != null ? itemDTO.getFeeHead() : "General Fee")
                        .standardAmount(stdAmt)
                        .applicableAmount(enabled ? stdAmt : BigDecimal.ZERO)
                        .mandatory(itemDTO.getMandatory() != null ? itemDTO.getMandatory() : false)
                        .enabled(enabled)
                        .build();
                feePlanItemRepository.save(item);
            }
        }

        feeAdjustmentRepository.deleteByFeePlanId(savedPlan.getId());
        if (request.getAdjustments() != null) {
            for (FeeAdjustmentDTO adjDTO : request.getAdjustments()) {
                FeeAdjustment adj = FeeAdjustment.builder()
                        .feePlanId(savedPlan.getId())
                        .studentId(savedPlan.getStudentId())
                        .adjustmentType(adjDTO.getAdjustmentType() != null ? adjDTO.getAdjustmentType() : "Discount")
                        .amount(adjDTO.getAmount() != null ? adjDTO.getAmount() : BigDecimal.ZERO)
                        .reason(adjDTO.getReason() != null ? adjDTO.getReason() : "Management concession")
                        .createdBy(userId)
                        .build();
                feeAdjustmentRepository.save(adj);
            }
        }

        // Synchronize with FeeRecord (CRITICAL: NEVER modifies FeePayment transactions)
        FeeRecord feeRecord = null;
        if (savedPlan.getFeeRecordId() != null) {
            feeRecord = feeRecordRepository.findById(savedPlan.getFeeRecordId()).orElse(null);
        }
        if (feeRecord == null) {
            feeRecord = feeRecordRepository.findByStudentId(savedPlan.getStudentId())
                    .stream()
                    .filter(r -> r.getAcademicYear() != null && r.getAcademicYear().equalsIgnoreCase(savedPlan.getAcademicYear()))
                    .findFirst()
                    .orElseGet(() -> FeeRecord.builder()
                            .studentId(savedPlan.getStudentId())
                            .studentName(savedPlan.getStudentName())
                            .studentClass(savedPlan.getStudentClass() + (savedPlan.getSection() != null ? "-" + savedPlan.getSection() : ""))
                            .feeType("Academic Fee Plan (" + savedPlan.getAcademicYear() + ")")
                            .paidAmount(BigDecimal.ZERO)
                            .status("Unpaid")
                            .build());
        }

        feeRecord.setFeePlanId(savedPlan.getId());
        feeRecord.setAcademicYear(savedPlan.getAcademicYear());
        feeRecord.setAmount(newFinalPayable);
        if (savedPlan.getDueDate() != null) feeRecord.setDueDate(savedPlan.getDueDate());

        // Verify paid amount directly from immutable payment transactions
        BigDecimal actualPaid = feeRecord.getId() != null ? feePaymentRepository.sumPaidByFeeRecordId(feeRecord.getId()) : BigDecimal.ZERO;
        feeRecord.setPaidAmount(actualPaid);

        if (actualPaid.compareTo(newFinalPayable) >= 0 && newFinalPayable.compareTo(BigDecimal.ZERO) > 0) {
            feeRecord.setStatus("Paid");
        } else if (actualPaid.compareTo(BigDecimal.ZERO) > 0) {
            feeRecord.setStatus("Partial");
        } else {
            feeRecord.setStatus("Unpaid");
        }

        FeeRecord savedRecord = feeRecordRepository.save(feeRecord);
        savedPlan.setFeeRecordId(savedRecord.getId());
        studentFeePlanRepository.save(savedPlan);

        log.info("Updated StudentFeePlan #{} for student #{} ({}) — New Payable: ₹{}, Prev: ₹{}",
                savedPlan.getId(), savedPlan.getStudentId(), savedPlan.getStudentName(), newFinalPayable, previousPayable);

        return buildPlanResponse(savedPlan);
    }

    @Override
    public List<StudentFeeSummaryDTO> getAllStudentFeeSummaries(String academicYear, String studentClass) {
        String year = (academicYear != null && !academicYear.isBlank()) ? academicYear.trim() : DEFAULT_ACADEMIC_YEAR;
        List<StudentFeePlan> plans;
        if (studentClass != null && !studentClass.isBlank()) {
            plans = studentFeePlanRepository.findByAcademicYearAndStudentClass(year, studentClass.trim());
        } else {
            plans = studentFeePlanRepository.findByAcademicYear(year);
        }

        return plans.stream().map(plan -> {
            BigDecimal paid = BigDecimal.ZERO;
            String payStatus = "UNPAID";

            if (plan.getFeeRecordId() != null) {
                FeeRecord rec = feeRecordRepository.findById(plan.getFeeRecordId()).orElse(null);
                if (rec != null) {
                    paid = rec.getPaidAmount() != null ? rec.getPaidAmount() : BigDecimal.ZERO;
                    payStatus = rec.getStatus() != null ? rec.getStatus().toUpperCase() : "UNPAID";
                }
            }

            BigDecimal payable = plan.getFinalPayableAmount() != null ? plan.getFinalPayableAmount() : BigDecimal.ZERO;
            BigDecimal outstanding = payable.subtract(paid);

            return StudentFeeSummaryDTO.builder()
                    .studentId(plan.getStudentId())
                    .studentName(plan.getStudentName())
                    .studentClass(plan.getStudentClass())
                    .section(plan.getSection())
                    .academicYear(plan.getAcademicYear())
                    .feePlanStatus(plan.getFeePlanStatus())
                    .paymentStatus(payStatus)
                    .standardFeeAmount(plan.getStandardFeeAmount())
                    .totalAdjustmentAmount(plan.getTotalAdjustmentAmount())
                    .finalPayableAmount(payable)
                    .paidAmount(paid)
                    .outstandingBalance(outstanding.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : outstanding)
                    .planId(plan.getId())
                    .feeRecordId(plan.getFeeRecordId())
                    .build();
        }).toList();
    }

    @Override
    public StudentFeePlanResponse getMyFeePlan(Long studentId) {
        return getStudentFeePlan(studentId, DEFAULT_ACADEMIC_YEAR);
    }

    @Override
    public List<FeePlanAuditDTO> getFeePlanAuditHistory(Long planId) {
        return feePlanAuditRepository.findByFeePlanIdOrderByChangedAtDesc(planId)
                .stream()
                .map(a -> FeePlanAuditDTO.builder()
                        .id(a.getId())
                        .previousAmount(a.getPreviousAmount())
                        .newAmount(a.getNewAmount())
                        .changeReason(a.getChangeReason())
                        .changedBy(a.getChangedBy())
                        .changedAt(a.getChangedAt())
                        .build())
                .toList();
    }

    private StudentFeePlanResponse buildPlanResponse(StudentFeePlan plan) {
        List<FeePlanItemDTO> items = feePlanItemRepository.findByFeePlanId(plan.getId())
                .stream()
                .map(i -> FeePlanItemDTO.builder()
                        .id(i.getId())
                        .feeHead(i.getFeeHead())
                        .standardAmount(i.getStandardAmount())
                        .applicableAmount(i.getApplicableAmount())
                        .mandatory(i.getMandatory())
                        .enabled(i.getEnabled())
                        .build())
                .toList();

        List<FeeAdjustmentDTO> adjustments = feeAdjustmentRepository.findByFeePlanId(plan.getId())
                .stream()
                .map(a -> FeeAdjustmentDTO.builder()
                        .id(a.getId())
                        .adjustmentType(a.getAdjustmentType())
                        .amount(a.getAmount())
                        .reason(a.getReason())
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList();

        List<FeePlanAuditDTO> audits = feePlanAuditRepository.findByFeePlanIdOrderByChangedAtDesc(plan.getId())
                .stream()
                .map(a -> FeePlanAuditDTO.builder()
                        .id(a.getId())
                        .previousAmount(a.getPreviousAmount())
                        .newAmount(a.getNewAmount())
                        .changeReason(a.getChangeReason())
                        .changedBy(a.getChangedBy())
                        .changedAt(a.getChangedAt())
                        .build())
                .toList();

        BigDecimal paid = BigDecimal.ZERO;
        String payStatus = "UNPAID";
        if (plan.getFeeRecordId() != null) {
            FeeRecord rec = feeRecordRepository.findById(plan.getFeeRecordId()).orElse(null);
            if (rec != null) {
                paid = rec.getPaidAmount() != null ? rec.getPaidAmount() : BigDecimal.ZERO;
                payStatus = rec.getStatus() != null ? rec.getStatus().toUpperCase() : "UNPAID";
            }
        }

        BigDecimal payable = plan.getFinalPayableAmount() != null ? plan.getFinalPayableAmount() : BigDecimal.ZERO;
        BigDecimal outstanding = payable.subtract(paid);

        return StudentFeePlanResponse.builder()
                .id(plan.getId())
                .studentId(plan.getStudentId())
                .studentName(plan.getStudentName())
                .studentClass(plan.getStudentClass())
                .section(plan.getSection())
                .academicYear(plan.getAcademicYear())
                .standardFeeAmount(plan.getStandardFeeAmount())
                .applicableAmount(plan.getApplicableAmount())
                .totalAdjustmentAmount(plan.getTotalAdjustmentAmount())
                .finalPayableAmount(payable)
                .paidAmount(paid)
                .outstandingBalance(outstanding.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : outstanding)
                .feePlanStatus(plan.getFeePlanStatus())
                .paymentStatus(payStatus)
                .dueDate(plan.getDueDate())
                .remarks(plan.getRemarks())
                .feeRecordId(plan.getFeeRecordId())
                .items(items)
                .adjustments(adjustments)
                .audits(audits)
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    // ── STATS ───────────────────────────────────────────────────

    @Override
    public FeeStatsResponse getStats() {
        List<FeeRecord> all = feeRecordRepository.findAll();
        return FeeStatsResponse.builder()
                .totalRevenue(feeRecordRepository.sumAllPaidAmount())
                .pendingAmount(feeRecordRepository.sumPendingAmount())
                .totalRecords(all.size())
                .paidCount(all.stream().filter(f -> "Paid".equalsIgnoreCase(f.getStatus())).count())
                .partialCount(all.stream().filter(f -> "Partial".equalsIgnoreCase(f.getStatus())).count())
                .unpaidCount(all.stream().filter(f -> "Unpaid".equalsIgnoreCase(f.getStatus())).count())
                .overdueCount(all.stream().filter(f -> "Overdue".equalsIgnoreCase(f.getStatus())).count())
                .build();
    }

    // ── PRIVATE HELPERS ─────────────────────────────────────────

    private synchronized String generateReceiptNumber() {
        int year = LocalDate.now().getYear();
        long seq = feePaymentRepository.countReceiptsByYear(year) + 1;
        String candidate = String.format("REC-%d-%05d", year, seq);

        while (feePaymentRepository.existsByReceiptNumber(candidate)) {
            seq++;
            candidate = String.format("REC-%d-%05d", year, seq);
        }
        return candidate;
    }
}
