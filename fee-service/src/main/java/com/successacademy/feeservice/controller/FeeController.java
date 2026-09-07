package com.successacademy.feeservice.controller;

import com.successacademy.feeservice.dto.FeeStatsResponse;
import com.successacademy.feeservice.dto.PaymentRequest;
import com.successacademy.feeservice.model.FeeRecord;
import com.successacademy.feeservice.model.FeeStructure;
import com.successacademy.feeservice.service.FeeService;
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

    // ── ADMIN — Fee Structure CRUD ─────────────────────────────

    @PostMapping("/structure")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeStructure addStructure(@RequestBody FeeStructure structure) {
        return service.addFeeStructure(structure);
    }

    @PutMapping("/structure/{id}")
    public FeeStructure updateStructure(@PathVariable Long id,
                                        @RequestBody FeeStructure structure) {
        return service.updateFeeStructure(id, structure);
    }

    @DeleteMapping("/structure/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStructure(@PathVariable Long id) {
        service.deleteFeeStructure(id);
    }

    // ── ADMIN — Fee Records ────────────────────────────────────

    // All fee records (admin list page)
    @GetMapping("/records")
    public ResponseEntity<List<FeeRecord>> getAllRecords() {
        return ResponseEntity.ok(service.getAllFeeRecords());
    }

    // Filter by status: Paid | Partial | Unpaid | Overdue
    @GetMapping("/records/status")
    public ResponseEntity<List<FeeRecord>> byStatus(@RequestParam String status) {
        return ResponseEntity.ok(service.getFeeRecordsByStatus(status));
    }

    // Add a new fee record (admin)
    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeRecord addRecord(@RequestBody FeeRecord record) {
        return service.addFeeRecord(record);
    }

    // Record a payment for a fee record
    @PostMapping("/records/pay")
    public ResponseEntity<FeeRecord> recordPayment(@RequestBody PaymentRequest request) {
        return ResponseEntity.ok(service.recordPayment(request));
    }

    @DeleteMapping("/records/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecord(@PathVariable Long id) {
        service.deleteFeeRecord(id);
    }

    // ── STUDENT PORTAL ─────────────────────────────────────────

    // Student's own fee records
    @GetMapping("/records/student/{studentId}")
    public ResponseEntity<List<FeeRecord>> byStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(service.getFeeRecordsByStudent(studentId));
    }

    // ── DASHBOARD STATS ────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<FeeStatsResponse> stats() {
        return ResponseEntity.ok(service.getStats());
    }
}
