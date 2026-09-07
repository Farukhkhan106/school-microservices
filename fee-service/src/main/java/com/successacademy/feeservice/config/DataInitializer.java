package com.successacademy.feeservice.config;

import com.successacademy.feeservice.model.FeeRecord;
import com.successacademy.feeservice.model.FeeStructure;
import com.successacademy.feeservice.repository.FeeRecordRepository;
import com.successacademy.feeservice.repository.FeeStructureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final FeeStructureRepository structureRepo;
    private final FeeRecordRepository recordRepo;

    @Override
    public void run(String... args) {

        // ── FEE STRUCTURES ─────────────────────────────────────
        if (structureRepo.count() == 0) {
            structureRepo.save(FeeStructure.builder()
                    .className("Class 1-5")
                    .tuitionFee(bd(15000)).transportFee(bd(3000))
                    .libraryFee(bd(500)).labFee(bd(0))
                    .sportsFee(bd(1000)).totalFee(bd(19500)).build());

            structureRepo.save(FeeStructure.builder()
                    .className("Class 6-8")
                    .tuitionFee(bd(20000)).transportFee(bd(3500))
                    .libraryFee(bd(750)).labFee(bd(1000))
                    .sportsFee(bd(1200)).totalFee(bd(26450)).build());

            structureRepo.save(FeeStructure.builder()
                    .className("Class 9-10")
                    .tuitionFee(bd(25000)).transportFee(bd(4000))
                    .libraryFee(bd(1000)).labFee(bd(1500))
                    .sportsFee(bd(1500)).totalFee(bd(33000)).build());

            structureRepo.save(FeeStructure.builder()
                    .className("Class 11-12 (Science)")
                    .tuitionFee(bd(35000)).transportFee(bd(4000))
                    .libraryFee(bd(1500)).labFee(bd(3000))
                    .sportsFee(bd(1500)).totalFee(bd(45000)).build());

            structureRepo.save(FeeStructure.builder()
                    .className("Class 11-12 (Commerce)")
                    .tuitionFee(bd(30000)).transportFee(bd(4000))
                    .libraryFee(bd(1500)).labFee(bd(1000))
                    .sportsFee(bd(1500)).totalFee(bd(38000)).build());

            structureRepo.save(FeeStructure.builder()
                    .className("Class 11-12 (Arts)")
                    .tuitionFee(bd(28000)).transportFee(bd(4000))
                    .libraryFee(bd(1500)).labFee(bd(500))
                    .sportsFee(bd(1500)).totalFee(bd(35500)).build());

            System.out.println("✅ Fee structures seeded — 6 records.");
        }

        // ── FEE RECORDS ────────────────────────────────────────
        if (recordRepo.count() == 0) {
            // Student 1 — Rahul Sharma — Class 10-A — Paid
            recordRepo.save(FeeRecord.builder()
                    .studentId(1L).studentName("Rahul Sharma").studentClass("10-A")
                    .feeType("Tuition").amount(bd(25000)).paidAmount(bd(25000))
                    .dueDate(LocalDate.of(2024, 12, 10))
                    .paymentDate(LocalDate.of(2024, 12, 5))
                    .status("Paid").paymentMethod("Online").build());

            // Student 2 — Priya Patel — Class 9-B — Partial
            recordRepo.save(FeeRecord.builder()
                    .studentId(2L).studentName("Priya Patel").studentClass("9-B")
                    .feeType("Tuition").amount(bd(22000)).paidAmount(bd(10000))
                    .dueDate(LocalDate.of(2024, 12, 10))
                    .paymentDate(LocalDate.of(2024, 12, 1))
                    .status("Partial").paymentMethod("Cash").build());

            // Student 3 — Arjun Singh — Class 8-A — Unpaid
            recordRepo.save(FeeRecord.builder()
                    .studentId(3L).studentName("Arjun Singh").studentClass("8-A")
                    .feeType("Tuition").amount(bd(20000)).paidAmount(bd(0))
                    .dueDate(LocalDate.of(2024, 12, 10))
                    .status("Unpaid").build());

            System.out.println("✅ Fee records seeded — 3 records.");
        }
    }

    private BigDecimal bd(long value) {
        return BigDecimal.valueOf(value);
    }
}
