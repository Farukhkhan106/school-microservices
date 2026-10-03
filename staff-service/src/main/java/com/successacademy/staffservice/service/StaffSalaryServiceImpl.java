package com.successacademy.staffservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.staffservice.dto.*;
import com.successacademy.staffservice.model.*;
import com.successacademy.staffservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffSalaryServiceImpl implements StaffSalaryService {

    private final StaffSalaryRepository salaryRepository;
    private final StaffMonthlySalaryRepository monthlySalaryRepository;
    private final StaffSalaryPaymentRepository paymentRepository;
    private final StaffRepository staffRepository;
    private final StaffAttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public StaffSalaryConfigResponse getSalaryConfig(Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + staffId));

        StaffSalary salary = salaryRepository.findByStaffId(staffId).orElse(null);
        return mapToConfigResponse(salary, staff);
    }

    @Override
    @Transactional
    public StaffSalaryConfigResponse saveSalaryConfig(Long staffId, StaffSalaryConfigRequest req, Long actorUserId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + staffId));

        StaffSalary salary = salaryRepository.findByStaffId(staffId).orElse(null);
        if (salary == null) {
            salary = StaffSalary.builder()
                    .staffId(staffId)
                    .build();
        }

        if (req.getSalaryType() != null && !req.getSalaryType().isBlank()) {
            salary.setSalaryType(req.getSalaryType().trim().toUpperCase());
        } else if (salary.getSalaryType() == null) {
            salary.setSalaryType("MONTHLY");
        }

        BigDecimal base = req.getBaseSalary() != null ? req.getBaseSalary() : BigDecimal.ZERO;
        if (base.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Base salary cannot be negative.");
        }

        salary.setBaseSalary(base);
        salary.setMonthlySalary(base);

        BigDecimal transport = req.getTransportAllowance() != null ? req.getTransportAllowance() : BigDecimal.ZERO;
        BigDecimal other = req.getOtherAllowance() != null ? req.getOtherAllowance() : BigDecimal.ZERO;
        BigDecimal fixedDed = req.getFixedDeduction() != null ? req.getFixedDeduction() : BigDecimal.ZERO;

        if (transport.compareTo(BigDecimal.ZERO) < 0 || other.compareTo(BigDecimal.ZERO) < 0 || fixedDed.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allowances and deductions cannot be negative.");
        }

        salary.setTransportAllowance(transport);
        salary.setOtherAllowance(other);
        salary.setFixedDeduction(fixedDed);
        salary.setEffectiveFrom(req.getEffectiveFrom() != null ? req.getEffectiveFrom() : LocalDate.now());
        salary.setEffectiveTo(req.getEffectiveTo());
        salary.setActive(true);
        salary.setRemarks(req.getRemarks());

        StaffSalary saved = salaryRepository.save(salary);

        auditLogService.log(actorUserId, staffId, "SALARY_CONFIG_SAVED", null, base.toString(), "Base Salary set to ₹" + base);

        return mapToConfigResponse(saved, staff);
    }

    @Override
    public List<StaffMonthlySalaryResponse> getMonthlySalaries(Integer month, Integer year) {
        LocalDate now = LocalDate.now();
        int m = (month != null) ? month : now.getMonthValue();
        int y = (year != null) ? year : now.getYear();

        List<StaffMonthlySalary> list = monthlySalaryRepository.findByMonthAndYearOrderByStaffIdAsc(m, y);
        Map<Long, Staff> staffMap = new HashMap<>();
        for (Staff s : staffRepository.findAll()) {
            staffMap.put(s.getId(), s);
        }

        List<StaffMonthlySalaryResponse> res = new ArrayList<>();
        for (StaffMonthlySalary monthly : list) {
            Staff staff = staffMap.get(monthly.getStaffId());
            res.add(mapToMonthlyResponse(monthly, staff));
        }
        return res;
    }

    @Override
    @Transactional
    public List<StaffMonthlySalaryResponse> calculateMonthlySalaries(CalculateSalaryRequest req, Long actorUserId) {
        LocalDate now = LocalDate.now();
        int month = (req != null && req.getMonth() != null) ? req.getMonth() : now.getMonthValue();
        int year = (req != null && req.getYear() != null) ? req.getYear() : now.getYear();
        int defaultWorkingDays = (req != null && req.getWorkingDays() != null && req.getWorkingDays() > 0)
                ? req.getWorkingDays() : 26;

        YearMonth ym = YearMonth.of(year, month);
        LocalDate startOfMonth = ym.atDay(1);
        LocalDate endOfMonth = ym.atEndOfMonth();

        List<Staff> targetStaffList;
        if (req != null && req.getStaffId() != null) {
            Staff s = staffRepository.findById(req.getStaffId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + req.getStaffId()));
            targetStaffList = List.of(s);
        } else {
            targetStaffList = staffRepository.findAll();
        }

        List<StaffMonthlySalaryResponse> result = new ArrayList<>();

        for (Staff staff : targetStaffList) {
            Optional<StaffMonthlySalary> existingOpt = monthlySalaryRepository.findByStaffIdAndMonthAndYear(staff.getId(), month, year);

            // If locked, cannot recalculate without explicit unlock
            if (existingOpt.isPresent() && Boolean.TRUE.equals(existingOpt.get().getLocked())) {
                result.add(mapToMonthlyResponse(existingOpt.get(), staff));
                continue;
            }

            StaffSalary salaryConfig = salaryRepository.findByStaffId(staff.getId()).orElse(null);
            BigDecimal baseSalary = (salaryConfig != null && salaryConfig.getBaseSalary() != null)
                    ? salaryConfig.getBaseSalary() : BigDecimal.ZERO;
            BigDecimal transportAllowance = (salaryConfig != null && salaryConfig.getTransportAllowance() != null)
                    ? salaryConfig.getTransportAllowance() : BigDecimal.ZERO;
            BigDecimal otherAllowance = (salaryConfig != null && salaryConfig.getOtherAllowance() != null)
                    ? salaryConfig.getOtherAllowance() : BigDecimal.ZERO;
            BigDecimal fixedDeduction = (salaryConfig != null && salaryConfig.getFixedDeduction() != null)
                    ? salaryConfig.getFixedDeduction() : BigDecimal.ZERO;

            // Attendance records for this staff in that month
            List<StaffAttendance> attendanceList = attendanceRepository
                    .findByStaffIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(staff.getId(), startOfMonth, endOfMonth);

            double presentDays = 0.0;
            double halfDayDays = 0.0;
            double leaveDays = 0.0;
            double absentDays = 0.0;

            for (StaffAttendance att : attendanceList) {
                String st = att.getStatus() != null ? att.getStatus().trim().toUpperCase() : "PRESENT";
                switch (st) {
                    case "PRESENT":
                        presentDays += 1.0;
                        break;
                    case "HALF_DAY":
                        halfDayDays += 1.0;
                        break;
                    case "LEAVE":
                        leaveDays += 1.0;
                        break;
                    case "ABSENT":
                        absentDays += 1.0;
                        break;
                    case "HOLIDAY":
                        presentDays += 1.0;
                        break;
                    case "WEEK_OFF":
                        presentDays += 1.0;
                        break;
                    default:
                        presentDays += 1.0;
                        break;
                }
            }

            // Approved leaves check (distinguish PAID vs UNPAID)
            List<LeaveRequest> approvedLeaves = leaveRequestRepository.findApprovedLeavesInDateRange(staff.getId(), startOfMonth, endOfMonth);
            double unpaidLeaveDays = 0.0;
            double paidLeaveDays = leaveDays;

            for (LeaveRequest lr : approvedLeaves) {
                if ("UNPAID".equalsIgnoreCase(lr.getLeaveType())) {
                    LocalDate from = lr.getFromDate().isBefore(startOfMonth) ? startOfMonth : lr.getFromDate();
                    LocalDate to = lr.getToDate().isAfter(endOfMonth) ? endOfMonth : lr.getToDate();
                    long days = ChronoUnit.DAYS.between(from, to) + 1;
                    unpaidLeaveDays += days;
                    if (paidLeaveDays >= days) {
                        paidLeaveDays -= days;
                    }
                }
            }

            // Check Proration (Joining date / Last working date)
            boolean prorated = false;
            String prorationRemarks = null;
            int effectiveWorkingDays = defaultWorkingDays;

            if (staff.getJoiningDate() != null && staff.getJoiningDate().isAfter(startOfMonth) && !staff.getJoiningDate().isAfter(endOfMonth)) {
                prorated = true;
                long totalCalDays = ym.lengthOfMonth();
                long eligibleCalDays = ChronoUnit.DAYS.between(staff.getJoiningDate(), endOfMonth) + 1;
                effectiveWorkingDays = (int) Math.round((double) defaultWorkingDays * ((double) eligibleCalDays / (double) totalCalDays));
                if (effectiveWorkingDays < 1) effectiveWorkingDays = 1;
                prorationRemarks = "Joined on " + staff.getJoiningDate() + " (Eligible: " + effectiveWorkingDays + "/" + defaultWorkingDays + " days)";
            }

            if (staff.getLastWorkingDate() != null && staff.getLastWorkingDate().isBefore(endOfMonth) && !staff.getLastWorkingDate().isBefore(startOfMonth)) {
                prorated = true;
                long totalCalDays = ym.lengthOfMonth();
                long eligibleCalDays = ChronoUnit.DAYS.between(startOfMonth, staff.getLastWorkingDate()) + 1;
                effectiveWorkingDays = (int) Math.round((double) defaultWorkingDays * ((double) eligibleCalDays / (double) totalCalDays));
                if (effectiveWorkingDays < 1) effectiveWorkingDays = 1;
                String exitNote = "Exited on " + staff.getLastWorkingDate() + " (Eligible: " + effectiveWorkingDays + "/" + defaultWorkingDays + " days)";
                prorationRemarks = (prorationRemarks != null) ? prorationRemarks + "; " + exitNote : exitNote;
            }

            // Deductible Unpaid Days: ONLY confirmed absences, unpaid leaves, and the 0.5 unpaid portion of half-days!
            // Missing/unmarked future days in an ongoing month are NOT treated as absences!
            double deductibleUnpaidDays = absentDays + unpaidLeaveDays + (halfDayDays * 0.5);

            double totalMarkedDays = presentDays + halfDayDays + leaveDays + absentDays;
            double unmarkedDays = Math.max(0.0, (double) effectiveWorkingDays - totalMarkedDays);

            // Payable days = effectiveWorkingDays - deductibleUnpaidDays
            double payableDays = Math.max(0.0, (double) effectiveWorkingDays - deductibleUnpaidDays);

            // Calculations
            BigDecimal grossSalary = baseSalary.add(transportAllowance).add(otherAllowance);
            BigDecimal perDayRate = BigDecimal.ZERO;
            if (defaultWorkingDays > 0 && baseSalary.compareTo(BigDecimal.ZERO) > 0) {
                perDayRate = baseSalary.divide(BigDecimal.valueOf(defaultWorkingDays), 4, RoundingMode.HALF_UP);
            }

            BigDecimal attendanceDeduction = perDayRate.multiply(BigDecimal.valueOf(deductibleUnpaidDays)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal netSalary = grossSalary.subtract(attendanceDeduction).subtract(fixedDeduction);
            if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
                netSalary = BigDecimal.ZERO;
            }

            // Snapshot config used
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("baseSalary", baseSalary);
            snapshot.put("transportAllowance", transportAllowance);
            snapshot.put("otherAllowance", otherAllowance);
            snapshot.put("fixedDeduction", fixedDeduction);
            snapshot.put("workingDays", effectiveWorkingDays);
            snapshot.put("perDayRate", perDayRate.setScale(2, RoundingMode.HALF_UP));
            snapshot.put("unmarkedDays", unmarkedDays);
            snapshot.put("deductibleUnpaidDays", deductibleUnpaidDays);
            String snapshotJson = "{}";
            try {
                snapshotJson = objectMapper.writeValueAsString(snapshot);
            } catch (Exception ignored) {}

            StaffMonthlySalary monthly = existingOpt.orElseGet(() -> StaffMonthlySalary.builder()
                    .staffId(staff.getId())
                    .month(month)
                    .year(year)
                    .paidAmount(BigDecimal.ZERO)
                    .paymentStatus("PENDING")
                    .payrollStatus("CALCULATED")
                    .build());

            monthly.setWorkingDays(effectiveWorkingDays);
            monthly.setPresentDays(presentDays + (halfDayDays * 0.5));
            monthly.setPaidLeaveDays(paidLeaveDays);
            monthly.setUnpaidLeaveDays(unpaidLeaveDays);
            monthly.setAbsentDays(absentDays);
            monthly.setUnmarkedDays(unmarkedDays);
            monthly.setGrossSalary(grossSalary);
            monthly.setAttendanceDeduction(attendanceDeduction);
            monthly.setOtherDeduction(fixedDeduction);
            monthly.setNetSalary(netSalary);
            monthly.setProrated(prorated);
            monthly.setProrationRemarks(prorationRemarks);
            monthly.setSalaryConfigSnapshot(snapshotJson);

            // Keep payments synchronized from payment ledger!
            if (monthly.getId() != null) {
                List<StaffSalaryPayment> payments = paymentRepository.findByMonthlySalaryIdOrderByCreatedAtAsc(monthly.getId());
                BigDecimal sumPaid = payments.stream()
                        .map(StaffSalaryPayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                monthly.setPaidAmount(sumPaid);
                if (sumPaid.compareTo(netSalary) >= 0 && netSalary.compareTo(BigDecimal.ZERO) > 0) {
                    monthly.setPaymentStatus("PAID");
                } else if (sumPaid.compareTo(BigDecimal.ZERO) > 0) {
                    monthly.setPaymentStatus("PARTIAL");
                } else {
                    monthly.setPaymentStatus("PENDING");
                }
            }

            StaffMonthlySalary saved = monthlySalaryRepository.save(monthly);
            result.add(mapToMonthlyResponse(saved, staff));
        }

        auditLogService.log(actorUserId, null, "PAYROLL_CALCULATED", null, null,
                "Calculated payroll for " + month + "/" + year + " (" + result.size() + " records)");

        return result;
    }

    @Override
    @Transactional
    public StaffMonthlySalaryResponse approvePayroll(Long monthlySalaryId, Long actorUserId) {
        StaffMonthlySalary monthly = monthlySalaryRepository.findById(monthlySalaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monthly salary record not found with id: " + monthlySalaryId));

        monthly.setPayrollStatus("APPROVED");
        monthly.setApprovedBy(actorUserId);
        monthly.setApprovedAt(LocalDateTime.now());

        StaffMonthlySalary saved = monthlySalaryRepository.save(monthly);
        Staff staff = staffRepository.findById(saved.getStaffId()).orElse(null);

        auditLogService.log(actorUserId, saved.getStaffId(), "PAYROLL_APPROVED", null, "APPROVED",
                "Approved payroll for " + saved.getMonth() + "/" + saved.getYear() + " (Net: ₹" + saved.getNetSalary() + ")");

        return mapToMonthlyResponse(saved, staff);
    }

    @Override
    @Transactional
    public List<StaffMonthlySalaryResponse> approveAllPayroll(Integer month, Integer year, Long actorUserId) {
        LocalDate now = LocalDate.now();
        int m = (month != null) ? month : now.getMonthValue();
        int y = (year != null) ? year : now.getYear();

        List<StaffMonthlySalary> list = monthlySalaryRepository.findByMonthAndYearOrderByStaffIdAsc(m, y);
        List<StaffMonthlySalaryResponse> res = new ArrayList<>();
        Map<Long, Staff> staffMap = new HashMap<>();
        for (Staff s : staffRepository.findAll()) staffMap.put(s.getId(), s);

        for (StaffMonthlySalary monthly : list) {
            if (!"LOCKED".equalsIgnoreCase(monthly.getPayrollStatus())) {
                monthly.setPayrollStatus("APPROVED");
                monthly.setApprovedBy(actorUserId);
                monthly.setApprovedAt(LocalDateTime.now());
                monthlySalaryRepository.save(monthly);
            }
            res.add(mapToMonthlyResponse(monthly, staffMap.get(monthly.getStaffId())));
        }

        auditLogService.log(actorUserId, null, "PAYROLL_APPROVED_ALL", null, null,
                "Approved all payroll records for " + m + "/" + y + " (" + list.size() + " records)");

        return res;
    }

    @Override
    @Transactional
    public StaffMonthlySalaryResponse unlockPayroll(Long monthlySalaryId, String reason, Long actorUserId) {
        StaffMonthlySalary monthly = monthlySalaryRepository.findById(monthlySalaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monthly salary record not found with id: " + monthlySalaryId));

        monthly.setLocked(false);
        monthly.setUnlockReason(reason != null ? reason : "Unlocked by admin");
        monthly.setPayrollStatus("CALCULATED");

        StaffMonthlySalary saved = monthlySalaryRepository.save(monthly);
        Staff staff = staffRepository.findById(saved.getStaffId()).orElse(null);

        auditLogService.log(actorUserId, saved.getStaffId(), "PAYROLL_UNLOCKED", "LOCKED", "CALCULATED",
                "Unlocked payroll: " + monthly.getUnlockReason());

        return mapToMonthlyResponse(saved, staff);
    }

    @Override
    @Transactional
    public StaffMonthlySalaryResponse markSalaryPaid(Long monthlySalaryId, Long actorUserId) {
        StaffMonthlySalary monthly = monthlySalaryRepository.findById(monthlySalaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monthly salary record not found with id: " + monthlySalaryId));

        BigDecimal net = monthly.getNetSalary() != null ? monthly.getNetSalary() : BigDecimal.ZERO;
        BigDecimal currentPaid = monthly.getPaidAmount() != null ? monthly.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = net.subtract(currentPaid);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            Staff staff = staffRepository.findById(monthly.getStaffId()).orElse(null);
            return mapToMonthlyResponse(monthly, staff);
        }

        StaffPaymentRequest req = StaffPaymentRequest.builder()
                .amount(remaining)
                .paymentMode("Cash")
                .paymentDate(LocalDate.now())
                .remarks("Marked full payment")
                .build();

        return recordSalaryPayment(monthlySalaryId, req, actorUserId);
    }

    @Override
    @Transactional
    public StaffMonthlySalaryResponse recordSalaryPayment(Long monthlySalaryId, StaffPaymentRequest req, Long actorUserId) {
        StaffMonthlySalary monthly = monthlySalaryRepository.findById(monthlySalaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monthly salary record not found with id: " + monthlySalaryId));

        if (Boolean.TRUE.equals(monthly.getLocked())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot record payment on a locked payroll.");
        }

        BigDecimal net = monthly.getNetSalary() != null ? monthly.getNetSalary() : BigDecimal.ZERO;
        BigDecimal currentPaid = monthly.getPaidAmount() != null ? monthly.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = net.subtract(currentPaid);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

        if (req == null || req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than ₹0.");
        }

        BigDecimal payAmount = req.getAmount();

        if (payAmount.compareTo(remaining) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment amount ₹" + payAmount + " exceeds outstanding balance of ₹" + remaining + ".");
        }

        // Generate sequential receipt number: SAL-{YEAR}-{0001}
        long receiptSeq = paymentRepository.count() + 1;
        String receiptNumber = String.format("SAL-%d-%04d", monthly.getYear(), receiptSeq);

        String mode = (req.getPaymentMode() != null && !req.getPaymentMode().isBlank()) ? req.getPaymentMode() : "Cash";
        LocalDate pDate = (req.getPaymentDate() != null) ? req.getPaymentDate() : LocalDate.now();
        String ref = (req.getPaymentReference() != null && !req.getPaymentReference().isBlank()) ? req.getPaymentReference() : receiptNumber;

        // 1. Create immutable ledger record
        StaffSalaryPayment payment = StaffSalaryPayment.builder()
                .monthlySalaryId(monthly.getId())
                .staffId(monthly.getStaffId())
                .amount(payAmount)
                .paymentDate(pDate)
                .paymentMode(mode)
                .referenceNumber(ref)
                .receiptNumber(receiptNumber)
                .remarks(req.getRemarks())
                .createdBy(actorUserId)
                .createdAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // 2. Re-calculate total paid from all payments
        BigDecimal newTotalPaid = currentPaid.add(payAmount);
        monthly.setPaidAmount(newTotalPaid);

        if (newTotalPaid.compareTo(net) >= 0) {
            monthly.setPaymentStatus("PAID");
        } else if (newTotalPaid.compareTo(BigDecimal.ZERO) > 0) {
            monthly.setPaymentStatus("PARTIAL");
        } else {
            monthly.setPaymentStatus("PENDING");
        }

        monthly.setPaidDate(pDate);
        monthly.setPaymentMode(mode);
        monthly.setPaymentReference(ref);
        if (req.getRemarks() != null && !req.getRemarks().isBlank()) {
            monthly.setRemarks(req.getRemarks());
        }

        StaffMonthlySalary saved = monthlySalaryRepository.save(monthly);
        Staff staff = staffRepository.findById(saved.getStaffId()).orElse(null);

        auditLogService.log(actorUserId, saved.getStaffId(), "SALARY_PAYMENT_RECORDED", currentPaid.toString(), newTotalPaid.toString(),
                "Payment of ₹" + payAmount + " via " + mode + " (Receipt: " + receiptNumber + ", Status: " + saved.getPaymentStatus() + ")");

        return mapToMonthlyResponse(saved, staff);
    }

    @Override
    public List<StaffSalaryPaymentResponse> getSalaryPayments(Long monthlySalaryId) {
        List<StaffSalaryPayment> list = paymentRepository.findByMonthlySalaryIdOrderByCreatedAtAsc(monthlySalaryId);
        StaffMonthlySalary monthly = monthlySalaryRepository.findById(monthlySalaryId).orElse(null);
        Staff staff = (monthly != null) ? staffRepository.findById(monthly.getStaffId()).orElse(null) : null;
        String staffName = (staff != null) ? (staff.getFirstName() + " " + staff.getLastName()).trim() : "";
        String staffCode = (staff != null) ? staff.getStaffCode() : "";

        List<StaffSalaryPaymentResponse> res = new ArrayList<>();
        for (StaffSalaryPayment p : list) {
            res.add(StaffSalaryPaymentResponse.builder()
                    .id(p.getId())
                    .monthlySalaryId(p.getMonthlySalaryId())
                    .staffId(p.getStaffId())
                    .staffName(staffName)
                    .staffCode(staffCode)
                    .amount(p.getAmount())
                    .paymentDate(p.getPaymentDate())
                    .paymentMode(p.getPaymentMode())
                    .referenceNumber(p.getReferenceNumber())
                    .receiptNumber(p.getReceiptNumber())
                    .remarks(p.getRemarks())
                    .createdBy(p.getCreatedBy())
                    .createdAt(p.getCreatedAt())
                    .build());
        }
        return res;
    }

    @Override
    public List<StaffMonthlySalaryResponse> getSalaryHistory(Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + staffId));

        List<StaffMonthlySalary> list = monthlySalaryRepository.findByStaffIdOrderByYearDescMonthDesc(staffId);
        List<StaffMonthlySalaryResponse> res = new ArrayList<>();
        for (StaffMonthlySalary m : list) {
            res.add(mapToMonthlyResponse(m, staff));
        }
        return res;
    }

    private StaffSalaryConfigResponse mapToConfigResponse(StaffSalary s, Staff staff) {
        String fullName = (staff != null) ? (staff.getFirstName() + " " + staff.getLastName()).trim() : "";
        String code = (staff != null) ? staff.getStaffCode() : "";
        String cat = (staff != null) ? staff.getCategory() : "";
        String desig = (staff != null) ? (staff.getDesignation() != null ? staff.getDesignation() : "") : "";

        if (s == null) {
            return StaffSalaryConfigResponse.builder()
                    .staffId(staff != null ? staff.getId() : null)
                    .staffName(fullName)
                    .staffCode(code)
                    .category(cat)
                    .designation(desig)
                    .salaryType("MONTHLY")
                    .baseSalary(BigDecimal.ZERO)
                    .transportAllowance(BigDecimal.ZERO)
                    .otherAllowance(BigDecimal.ZERO)
                    .fixedDeduction(BigDecimal.ZERO)
                    .grossSalary(BigDecimal.ZERO)
                    .netSalary(BigDecimal.ZERO)
                    .active(false)
                    .build();
        }

        BigDecimal base = s.getBaseSalary() != null ? s.getBaseSalary() : (s.getMonthlySalary() != null ? s.getMonthlySalary() : BigDecimal.ZERO);
        BigDecimal transport = s.getTransportAllowance() != null ? s.getTransportAllowance() : BigDecimal.ZERO;
        BigDecimal other = s.getOtherAllowance() != null ? s.getOtherAllowance() : BigDecimal.ZERO;
        BigDecimal fixedDed = s.getFixedDeduction() != null ? s.getFixedDeduction() : BigDecimal.ZERO;
        BigDecimal gross = base.add(transport).add(other);
        BigDecimal net = gross.subtract(fixedDed);
        if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;

        return StaffSalaryConfigResponse.builder()
                .id(s.getId())
                .staffId(s.getStaffId())
                .staffName(fullName)
                .staffCode(code)
                .category(cat)
                .designation(desig)
                .salaryType(s.getSalaryType() != null ? s.getSalaryType() : "MONTHLY")
                .baseSalary(base)
                .transportAllowance(transport)
                .otherAllowance(other)
                .fixedDeduction(fixedDed)
                .grossSalary(gross)
                .netSalary(net)
                .effectiveFrom(s.getEffectiveFrom())
                .effectiveTo(s.getEffectiveTo())
                .active(s.getActive() != null ? s.getActive() : true)
                .remarks(s.getRemarks())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private StaffMonthlySalaryResponse mapToMonthlyResponse(StaffMonthlySalary m, Staff staff) {
        String fullName = (staff != null) ? (staff.getFirstName() + " " + staff.getLastName()).trim() : "";
        String code = (staff != null) ? staff.getStaffCode() : "";
        String cat = (staff != null) ? staff.getCategory() : "";
        String desig = (staff != null) ? (staff.getDesignation() != null ? staff.getDesignation() : "") : "";
        String phone = (staff != null) ? staff.getPhone() : "";

        BigDecimal base = BigDecimal.ZERO;
        if (m.getSalaryConfigSnapshot() != null && !m.getSalaryConfigSnapshot().isBlank()) {
            try {
                Map<?, ?> map = objectMapper.readValue(m.getSalaryConfigSnapshot(), Map.class);
                if (map.containsKey("baseSalary")) {
                    base = new BigDecimal(String.valueOf(map.get("baseSalary")));
                }
            } catch (Exception ignored) {}
        }
        if (base.compareTo(BigDecimal.ZERO) == 0 && m.getGrossSalary() != null) {
            base = m.getGrossSalary();
        }

        BigDecimal net = m.getNetSalary() != null ? m.getNetSalary() : BigDecimal.ZERO;
        BigDecimal paid = m.getPaidAmount() != null ? m.getPaidAmount()
                : ("PAID".equalsIgnoreCase(m.getPaymentStatus()) ? net : BigDecimal.ZERO);
        BigDecimal rem = net.subtract(paid);
        if (rem.compareTo(BigDecimal.ZERO) < 0) rem = BigDecimal.ZERO;

        return StaffMonthlySalaryResponse.builder()
                .id(m.getId())
                .staffId(m.getStaffId())
                .staffName(fullName)
                .staffCode(code)
                .category(cat)
                .designation(desig)
                .phone(phone)
                .month(m.getMonth())
                .year(m.getYear())
                .workingDays(m.getWorkingDays())
                .presentDays(m.getPresentDays())
                .paidLeaveDays(m.getPaidLeaveDays())
                .unpaidLeaveDays(m.getUnpaidLeaveDays())
                .absentDays(m.getAbsentDays())
                .unmarkedDays(m.getUnmarkedDays() != null ? m.getUnmarkedDays() : 0.0)
                .baseSalary(base)
                .grossSalary(m.getGrossSalary())
                .attendanceDeduction(m.getAttendanceDeduction())
                .otherDeduction(m.getOtherDeduction())
                .netSalary(net)
                .paidAmount(paid)
                .remainingAmount(rem)
                .paymentStatus(m.getPaymentStatus())
                .paidDate(m.getPaidDate())
                .paymentMode(m.getPaymentMode())
                .paymentReference(m.getPaymentReference())
                .payrollStatus(m.getPayrollStatus() != null ? m.getPayrollStatus() : "CALCULATED")
                .approvedBy(m.getApprovedBy())
                .approvedAt(m.getApprovedAt())
                .locked(m.getLocked() != null ? m.getLocked() : false)
                .prorated(m.getProrated() != null ? m.getProrated() : false)
                .prorationRemarks(m.getProrationRemarks())
                .salaryConfigSnapshot(m.getSalaryConfigSnapshot())
                .remarks(m.getRemarks())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
