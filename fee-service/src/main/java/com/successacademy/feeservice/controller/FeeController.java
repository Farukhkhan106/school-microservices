package com.successacademy.feeservice.controller;

import com.successacademy.feeservice.dto.FeePlanDTOs.*;
import com.successacademy.feeservice.dto.FeeStatsResponse;
import com.successacademy.feeservice.dto.PaymentRequest;
import com.successacademy.feeservice.dto.PaymentResponse;
import com.successacademy.feeservice.dto.ReceiptResponse;
import com.successacademy.feeservice.model.FeeRecord;
import com.successacademy.feeservice.model.FeeStructure;
import com.successacademy.feeservice.security.UserContext;
import com.successacademy.feeservice.service.FeeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fees")
@RequiredArgsConstructor
public class FeeController {

    private final FeeService service;

    // ── PUBLIC (NO AUTH — whitelisted in gateway) ──────────────

    // Fee structure for public website fees page
    @GetMapping("/structure")
    public ResponseEntity<List<FeeStructure>> getAllStructures() {
        return ResponseEntity.ok(service.getAllFeeStructures());
    }

    @GetMapping("/structure/{className}")
    public ResponseEntity<FeeStructure> getStructureByClass(
            @PathVariable String className) {
        return ResponseEntity.ok(service.getFeeStructureByClass(className));
    }

    // ── ADMIN ONLY — Fee Structure CRUD ────────────────────────

    @PostMapping("/structure")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeStructure addStructure(@RequestBody FeeStructure structure, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.addFeeStructure(structure);
    }

    @PutMapping("/structure/{id}")
    public FeeStructure updateStructure(@PathVariable Long id,
                                        @RequestBody FeeStructure structure,
                                        HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.updateFeeStructure(id, structure);
    }

    @DeleteMapping("/structure/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStructure(@PathVariable Long id, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        service.deleteFeeStructure(id);
    }

    // ── ADMIN ONLY — Fee Records Management ────────────────────

    // All fee records (admin list page)
    @GetMapping("/records")
    public ResponseEntity<List<FeeRecord>> getAllRecords(HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return ResponseEntity.ok(service.getAllFeeRecords());
    }

    // Filter by status: Paid | Partial | Unpaid | Overdue
    @GetMapping("/records/status")
    public ResponseEntity<List<FeeRecord>> byStatus(@RequestParam String status, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return ResponseEntity.ok(service.getFeeRecordsByStatus(status));
    }

    // Add a new fee record (admin)
    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeRecord addRecord(@RequestBody FeeRecord record, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return service.addFeeRecord(record);
    }

    // Record a payment for a fee record (admin only)
    @PostMapping("/records/pay")
    public ResponseEntity<PaymentResponse> recordPayment(
            @RequestBody PaymentRequest paymentRequest,
            HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        if (paymentRequest.getRecordedBy() == null) {
            paymentRequest.setRecordedBy(UserContext.userId(request));
        }
        return ResponseEntity.ok(service.recordPayment(paymentRequest));
    }

    // Delete fee record (delete protection enforced in service)
    @DeleteMapping("/records/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecord(@PathVariable Long id, HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        service.deleteFeeRecord(id);
    }

    // ── STUDENT FEE PLANS (Customized Billing) ───────────────────

    // Get class fee template with configurable heads (Admin only)
    @GetMapping("/plans/template/{studentClass}")
    public ResponseEntity<ClassFeeTemplateResponse> getClassFeeTemplate(
            @PathVariable String studentClass,
            @RequestParam(required = false) String academicYear,
            HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return ResponseEntity.ok(service.getClassFeeTemplate(studentClass, academicYear));
    }

    // Get a student's customized fee plan (Admin or owning Student)
    @GetMapping("/plans/student/{studentId}")
    public ResponseEntity<StudentFeePlanResponse> getStudentFeePlan(
            @PathVariable Long studentId,
            @RequestParam(required = false) String academicYear,
            HttpServletRequest request) {
        UserContext.requireStudentOwnershipOrAdmin(request, studentId);
        return ResponseEntity.ok(service.getStudentFeePlan(studentId, academicYear));
    }

    // Save/Configure a student's fee plan (Admin only)
    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<StudentFeePlanResponse> saveStudentFeePlan(
            @RequestBody StudentFeePlanRequest planRequest,
            HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        Long adminId = UserContext.userId(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveStudentFeePlan(planRequest, adminId));
    }

    // Update/Edit an existing student fee plan (Admin only)
    @PutMapping("/plans/{id}")
    public ResponseEntity<StudentFeePlanResponse> updateStudentFeePlan(
            @PathVariable Long id,
            @RequestBody StudentFeePlanRequest planRequest,
            HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        Long adminId = UserContext.userId(request);
        return ResponseEntity.ok(service.updateStudentFeePlan(id, planRequest, adminId));
    }

    // List all student fee plan summaries (Admin only)
    @GetMapping("/plans/all")
    public ResponseEntity<List<StudentFeeSummaryDTO>> getAllStudentFeeSummaries(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String studentClass,
            HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return ResponseEntity.ok(service.getAllStudentFeeSummaries(academicYear, studentClass));
    }

    // Audit history for a fee plan (Admin only)
    @GetMapping("/plans/{id}/history")
    public ResponseEntity<List<FeePlanAuditDTO>> getFeePlanAuditHistory(
            @PathVariable Long id,
            HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return ResponseEntity.ok(service.getFeePlanAuditHistory(id));
    }

    // Student self-view of their own fee plan
    @GetMapping("/my-plan")
    public ResponseEntity<StudentFeePlanResponse> getMyFeePlan(HttpServletRequest request) {
        UserContext.requireRole(request, "STUDENT");
        Long studentId = UserContext.studentId(request);
        return ResponseEntity.ok(service.getMyFeePlan(studentId));
    }

    // ── STUDENT & ADMIN ACCESS — Records, Payment Ledger & Receipts ─

    // View single fee record (Admin or owning Student)
    @GetMapping("/records/{id}")
    public ResponseEntity<FeeRecord> getRecordById(@PathVariable Long id, HttpServletRequest request) {
        FeeRecord record = service.getFeeRecordById(id);
        UserContext.requireStudentOwnershipOrAdmin(request, record.getStudentId());
        return ResponseEntity.ok(record);
    }

    // Student's own fee records
    @GetMapping("/records/student/{studentId}")
    public ResponseEntity<List<FeeRecord>> byStudent(@PathVariable Long studentId, HttpServletRequest request) {
        UserContext.requireStudentOwnershipOrAdmin(request, studentId);
        return ResponseEntity.ok(service.getFeeRecordsByStudent(studentId));
    }

    // Payment history ledger for a specific fee record
    @GetMapping("/payments/record/{feeRecordId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(
            @PathVariable Long feeRecordId,
            HttpServletRequest request) {
        FeeRecord record = service.getFeeRecordById(feeRecordId);
        UserContext.requireStudentOwnershipOrAdmin(request, record.getStudentId());
        return ResponseEntity.ok(service.getPaymentHistory(feeRecordId));
    }

    // Receipt by Payment ID
    @GetMapping("/payments/{paymentId}/receipt")
    public ResponseEntity<ReceiptResponse> getReceipt(
            @PathVariable Long paymentId,
            HttpServletRequest request) {
        ReceiptResponse receipt = service.getReceipt(paymentId);
        UserContext.requireStudentOwnershipOrAdmin(request, receipt.getStudentId());
        return ResponseEntity.ok(receipt);
    }

    // Receipt by Receipt Number
    @GetMapping("/payments/receipt/{receiptNumber}")
    public ResponseEntity<ReceiptResponse> getReceiptByNumber(
            @PathVariable String receiptNumber,
            HttpServletRequest request) {
        ReceiptResponse receipt = service.getReceiptByNumber(receiptNumber);
        UserContext.requireStudentOwnershipOrAdmin(request, receipt.getStudentId());
        return ResponseEntity.ok(receipt);
    }

    // ── DASHBOARD STATS (Admin only) ───────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<FeeStatsResponse> stats(HttpServletRequest request) {
        UserContext.requireRole(request, "ADMIN");
        return ResponseEntity.ok(service.getStats());
    }
}
