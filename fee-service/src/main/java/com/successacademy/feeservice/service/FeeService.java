package com.successacademy.feeservice.service;

import com.successacademy.feeservice.dto.FeeStatsResponse;
import com.successacademy.feeservice.dto.PaymentRequest;
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
    List<FeeRecord> getFeeRecordsByStudent(Long studentId);
    List<FeeRecord> getFeeRecordsByStatus(String status);
    FeeRecord addFeeRecord(FeeRecord record);
    FeeRecord recordPayment(PaymentRequest request);
    void deleteFeeRecord(Long id);

    // ── STATS (dashboard) ───────────────────────────────────────
    FeeStatsResponse getStats();
}
