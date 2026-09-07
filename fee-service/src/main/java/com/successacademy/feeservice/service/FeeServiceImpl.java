package com.successacademy.feeservice.service;

import com.successacademy.feeservice.dto.FeeStatsResponse;
import com.successacademy.feeservice.dto.PaymentRequest;
import com.successacademy.feeservice.model.FeeRecord;
import com.successacademy.feeservice.model.FeeStructure;
import com.successacademy.feeservice.repository.FeeRecordRepository;
import com.successacademy.feeservice.repository.FeeStructureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeeServiceImpl implements FeeService {

    private final FeeRecordRepository feeRecordRepository;
    private final FeeStructureRepository feeStructureRepository;

    // ── FEE STRUCTURE ──────────────────────────────────────────

    @Override
    public List<FeeStructure> getAllFeeStructures() {
        return feeStructureRepository.findAll();
    }

    @Override
    public FeeStructure getFeeStructureByClass(String className) {
        return feeStructureRepository.findByClassNameIgnoreCase(className)
                .orElseThrow(() -> new RuntimeException("Fee structure not found for class: " + className));
    }

    @Override
    public FeeStructure addFeeStructure(FeeStructure structure) {
        return feeStructureRepository.save(structure);
    }

    @Override
    public FeeStructure updateFeeStructure(Long id, FeeStructure structure) {
        FeeStructure existing = feeStructureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fee structure not found with id: " + id));
        existing.setClassName(structure.getClassName());
        existing.setTuitionFee(structure.getTuitionFee());
        existing.setTransportFee(structure.getTransportFee());
        existing.setLibraryFee(structure.getLibraryFee());
        existing.setLabFee(structure.getLabFee());
        existing.setSportsFee(structure.getSportsFee());
        existing.setTotalFee(structure.getTotalFee());
        return feeStructureRepository.save(existing);
    }

    @Override
    public void deleteFeeStructure(Long id) {
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
    public List<FeeRecord> getFeeRecordsByStudent(Long studentId) {
        return feeRecordRepository.findByStudentId(studentId);
    }

    @Override
    public List<FeeRecord> getFeeRecordsByStatus(String status) {
        return feeRecordRepository.findByStatus(status);
    }

    @Override
    public FeeRecord addFeeRecord(FeeRecord record) {
        if (record.getStatus() == null) {
            record.setStatus("Unpaid");
        }
        if (record.getPaidAmount() == null) {
            record.setPaidAmount(BigDecimal.ZERO);
        }
        return feeRecordRepository.save(record);
    }

    @Override
    public FeeRecord recordPayment(PaymentRequest request) {
        FeeRecord record = feeRecordRepository.findById(request.getFeeRecordId())
                .orElseThrow(() -> new RuntimeException("Fee record not found with id: " + request.getFeeRecordId()));

        // Add payment to paidAmount
        BigDecimal newPaid = record.getPaidAmount().add(request.getPaymentAmount());
        record.setPaidAmount(newPaid);
        record.setPaymentMethod(request.getPaymentMethod());
        record.setPaymentDate(request.getPaymentDate() != null
                ? request.getPaymentDate()
                : LocalDate.now());
        if (request.getRemarks() != null) {
            record.setRemarks(request.getRemarks());
        }

        // Auto-update status
        int cmp = newPaid.compareTo(record.getAmount());
        if (cmp >= 0) {
            record.setStatus("Paid");
        } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
            record.setStatus("Partial");
        }

        return feeRecordRepository.save(record);
    }

    @Override
    public void deleteFeeRecord(Long id) {
        feeRecordRepository.deleteById(id);
    }

    // ── STATS ───────────────────────────────────────────────────

    @Override
    public FeeStatsResponse getStats() {
        List<FeeRecord> all = feeRecordRepository.findAll();
        return FeeStatsResponse.builder()
                .totalRevenue(feeRecordRepository.sumAllPaidAmount())
                .pendingAmount(feeRecordRepository.sumPendingAmount())
                .totalRecords(all.size())
                .paidCount(all.stream().filter(f -> "Paid".equals(f.getStatus())).count())
                .partialCount(all.stream().filter(f -> "Partial".equals(f.getStatus())).count())
                .unpaidCount(all.stream().filter(f -> "Unpaid".equals(f.getStatus())).count())
                .overdueCount(all.stream().filter(f -> "Overdue".equals(f.getStatus())).count())
                .build();
    }
}
