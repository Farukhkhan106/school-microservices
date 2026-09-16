package com.successacademy.feeservice.service;

import com.successacademy.feeservice.dto.FeePlanDTOs.*;
import com.successacademy.feeservice.dto.FeeStatsResponse;
import com.successacademy.feeservice.dto.PaymentRequest;
import com.successacademy.feeservice.dto.PaymentResponse;
import com.successacademy.feeservice.dto.ReceiptResponse;
import com.successacademy.feeservice.model.FeeRecord;
import com.successacademy.feeservice.model.FeeStructure;

import java.util.List;

public interface FeeService {

    // ── FEE STRUCTURE ──────────────────────────────────────────
    List<FeeStructure> getAllFeeStructures();
    FeeStructure getFeeStructureByClass(String className);
    FeeStructure addFeeStructure(FeeStructure structure);
    FeeStructure updateFeeStructure(Long id, FeeStructure structure);
    void deleteFeeStructure(Long id);

    // ── FEE RECORDS ─────────────────────────────────────────────
    List<FeeRecord> getAllFeeRecords();
    FeeRecord getFeeRecordById(Long id);
    List<FeeRecord> getFeeRecordsByStudent(Long studentId);
    List<FeeRecord> getFeeRecordsByStatus(String status);
    FeeRecord addFeeRecord(FeeRecord record);
    PaymentResponse recordPayment(PaymentRequest request);
    void deleteFeeRecord(Long id);

    // ── PAYMENT LEDGER & RECEIPTS ───────────────────────────────
    List<PaymentResponse> getPaymentHistory(Long feeRecordId);
    ReceiptResponse getReceipt(Long paymentId);
    ReceiptResponse getReceiptByNumber(String receiptNumber);

    // ── STUDENT FEE PLANS (Customized Billing) ───────────────────
    ClassFeeTemplateResponse getClassFeeTemplate(String studentClass, String academicYear);
    StudentFeePlanResponse getStudentFeePlan(Long studentId, String academicYear);
    StudentFeePlanResponse saveStudentFeePlan(StudentFeePlanRequest request, Long userId);
    StudentFeePlanResponse updateStudentFeePlan(Long planId, StudentFeePlanRequest request, Long userId);
    List<StudentFeeSummaryDTO> getAllStudentFeeSummaries(String academicYear, String studentClass);
    StudentFeePlanResponse getMyFeePlan(Long studentId);
    List<FeePlanAuditDTO> getFeePlanAuditHistory(Long planId);

    // ── STATS (dashboard) ───────────────────────────────────────
    FeeStatsResponse getStats();
}
