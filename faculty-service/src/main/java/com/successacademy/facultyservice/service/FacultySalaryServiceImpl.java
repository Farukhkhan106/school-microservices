package com.successacademy.facultyservice.service;

import com.successacademy.facultyservice.dto.FacultyCalculateSalaryRequest;
import com.successacademy.facultyservice.dto.FacultyMonthlySalaryResponse;
import com.successacademy.facultyservice.dto.FacultyPaymentRequest;
import com.successacademy.facultyservice.dto.FacultySalaryPaymentResponse;
import com.successacademy.facultyservice.model.Faculty;
import com.successacademy.facultyservice.model.FacultyAttendance;
import com.successacademy.facultyservice.model.FacultyLeaveRequest;
import com.successacademy.facultyservice.model.FacultyMonthlySalary;
import com.successacademy.facultyservice.model.FacultySalaryPayment;
import com.successacademy.facultyservice.repository.FacultyAttendanceRepository;
import com.successacademy.facultyservice.repository.FacultyLeaveRepository;
import com.successacademy.facultyservice.repository.FacultyMonthlySalaryRepository;
import com.successacademy.facultyservice.repository.FacultyRepository;
import com.successacademy.facultyservice.repository.FacultySalaryPaymentRepository;
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
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacultySalaryServiceImpl implements FacultySalaryService {

    private final FacultyMonthlySalaryRepository monthlySalaryRepository;
    private final FacultySalaryPaymentRepository paymentRepository;
    private final FacultyRepository facultyRepository;
    private final FacultyAttendanceRepository attendanceRepository;
    private final FacultyLeaveRepository leaveRepository;

    @Override
    @Transactional
    public List<FacultyMonthlySalaryResponse> calculateMonthlySalaries(FacultyCalculateSalaryRequest req, Long actorUserId) {
        LocalDate now = LocalDate.now();
        int month = (req != null && req.getMonth() != null) ? req.getMonth() : now.getMonthValue();
        int year = (req != null && req.getYear() != null) ? req.getYear() : now.getYear();
        int workingDays = (req != null && req.getWorkingDays() != null && req.getWorkingDays() > 0) ? req.getWorkingDays() : 26;

        List<Faculty> targets;
        if (req != null && req.getFacultyId() != null) {
            targets = facultyRepository.findById(req.getFacultyId()).map(List::of).orElse(List.of());
        } else {
            targets = facultyRepository.findByStatusIgnoreCase("Active");
        }

        List<FacultyMonthlySalaryResponse> responses = new ArrayList<>();

        for (Faculty f : targets) {
            BigDecimal baseSalary = (f.getBaseSalary() != null && f.getBaseSalary().compareTo(BigDecimal.ZERO) > 0)
                    ? f.getBaseSalary()
                    : BigDecimal.valueOf(26000.00);

            BigDecimal perDayRate = baseSalary.divide(BigDecimal.valueOf(workingDays), 2, RoundingMode.HALF_UP);

            // Fetch attendance records for this faculty in the target month & year
            List<FacultyAttendance> attList = attendanceRepository.findByFacultyIdAndYearAndMonth(f.getId(), year, month);

            double presentDays = 0.0;
            double halfDays = 0.0;
            double absentDays = 0.0;
            double weekOffDays = 0.0;
            double holidayDays = 0.0;
            double leaveDays = 0.0;

            for (FacultyAttendance att : attList) {
                String st = (att.getStatus() != null) ? att.getStatus().toUpperCase() : "";
                switch (st) {
                    case "PRESENT" -> presentDays += 1.0;
                    case "HALF_DAY" -> {
                        halfDays += 1.0;
                        presentDays += 0.5; // 0.5 counted as working
                    }
                    case "ABSENT" -> absentDays += 1.0;
                    case "WEEK_OFF" -> {
                        weekOffDays += 1.0;
                        presentDays += 1.0; // paid week-off
                    }
                    case "HOLIDAY" -> {
                        holidayDays += 1.0;
                        presentDays += 1.0; // paid holiday
                    }
                    case "LEAVE" -> leaveDays += 1.0;
                }
            }

            // Differentiate Paid vs Unpaid Leaves for this month
            LocalDate monthStart = LocalDate.of(year, month, 1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            List<FacultyLeaveRequest> leaves = leaveRepository.findApprovedLeavesOverlapping(f.getId(), monthStart, monthEnd);

            double paidLeaveDays = 0.0;
            double unpaidLeaveDays = 0.0;

            for (FacultyLeaveRequest l : leaves) {
                LocalDate start = l.getFromDate().isBefore(monthStart) ? monthStart : l.getFromDate();
                LocalDate end = l.getToDate().isAfter(monthEnd) ? monthEnd : l.getToDate();

                long daysCount = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
                if ("UNPAID".equalsIgnoreCase(l.getLeaveType())) {
                    unpaidLeaveDays += daysCount;
                } else {
                    paidLeaveDays += daysCount;
                }
            }

            // Total LOP (Loss of Pay) Days
            // PRESENT = 0 deduction
            // HALF_DAY = 0.5 day deduction
            // ABSENT = 1 day deduction
            // UNPAID LEAVE = 1 day deduction
            // WEEK_OFF = 0 deduction
            // HOLIDAY = 0 deduction
            // PAID LEAVE = 0 deduction
            // Future/unmarked days = 0 deduction
            double lopDays = absentDays + (halfDays * 0.5) + unpaidLeaveDays;

            BigDecimal attendanceDeduction = BigDecimal.valueOf(lopDays).multiply(perDayRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal grossSalary = baseSalary;
            BigDecimal otherDeduction = BigDecimal.ZERO;

            BigDecimal netSalary = grossSalary.subtract(attendanceDeduction).subtract(otherDeduction);
            if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
                netSalary = BigDecimal.ZERO;
            }

            // Check if monthly salary already exists
            Optional<FacultyMonthlySalary> existingOpt = monthlySalaryRepository.findByFacultyIdAndMonthAndYear(f.getId(), month, year);

            FacultyMonthlySalary monthly;
            if (existingOpt.isPresent()) {
                monthly = existingOpt.get();
                if ("LOCKED".equalsIgnoreCase(monthly.getPayrollStatus())) {
                    responses.add(mapToResponse(monthly, f));
                    continue;
                }
                monthly.setWorkingDays(workingDays);
                monthly.setPresentDays(presentDays);
                monthly.setHalfDays(halfDays);
                monthly.setAbsentDays(absentDays);
                monthly.setPaidLeaveDays(paidLeaveDays);
                monthly.setUnpaidLeaveDays(unpaidLeaveDays);
                monthly.setBaseSalary(baseSalary);
                monthly.setGrossSalary(grossSalary);
                monthly.setAttendanceDeduction(attendanceDeduction);
                monthly.setOtherDeduction(otherDeduction);
                monthly.setNetSalary(netSalary);

                BigDecimal currentPaid = (monthly.getPaidAmount() != null) ? monthly.getPaidAmount() : BigDecimal.ZERO;
                BigDecimal remaining = netSalary.subtract(currentPaid);
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    remaining = BigDecimal.ZERO;
                    monthly.setPaymentStatus("PAID");
                } else if (currentPaid.compareTo(BigDecimal.ZERO) > 0) {
                    monthly.setPaymentStatus("PARTIAL");
                } else {
                    monthly.setPaymentStatus("PENDING");
                }
                monthly.setRemainingAmount(remaining);
            } else {
                monthly = FacultyMonthlySalary.builder()
                        .facultyId(f.getId())
                        .month(month)
                        .year(year)
                        .workingDays(workingDays)
                        .presentDays(presentDays)
                        .halfDays(halfDays)
                        .absentDays(absentDays)
                        .paidLeaveDays(paidLeaveDays)
                        .unpaidLeaveDays(unpaidLeaveDays)
                        .baseSalary(baseSalary)
                        .grossSalary(grossSalary)
                        .attendanceDeduction(attendanceDeduction)
                        .otherDeduction(otherDeduction)
                        .netSalary(netSalary)
                        .paidAmount(BigDecimal.ZERO)
                        .remainingAmount(netSalary)
                        .paymentStatus("PENDING")
                        .payrollStatus("CALCULATED")
                        .build();
            }

            String snapshot = String.format(
                    "{\"baseSalary\":%.2f,\"workingDays\":%d,\"perDayRate\":%.2f,\"deductibleUnpaidDays\":%.1f}",
                    baseSalary.doubleValue(), workingDays, perDayRate.doubleValue(), lopDays
            );
            monthly.setSalaryConfigSnapshot(snapshot);

            FacultyMonthlySalary saved = monthlySalaryRepository.save(monthly);
            responses.add(mapToResponse(saved, f));
        }

        return responses;
    }

    @Override
    public List<FacultyMonthlySalaryResponse> getMonthlySalaries(Integer month, Integer year) {
        LocalDate now = LocalDate.now();
        int m = (month != null) ? month : now.getMonthValue();
        int y = (year != null) ? year : now.getYear();

        List<FacultyMonthlySalary> list = monthlySalaryRepository.findByMonthAndYear(m, y);
        return list.stream().map(sal -> {
            Faculty f = facultyRepository.findById(sal.getFacultyId()).orElse(null);
            return mapToResponse(sal, f);
        }).toList();
    }

    @Override
    public FacultyMonthlySalaryResponse getMonthlySalaryById(Long id) {
        FacultyMonthlySalary sal = monthlySalaryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monthly salary not found with id: " + id));
        Faculty f = facultyRepository.findById(sal.getFacultyId()).orElse(null);
        return mapToResponse(sal, f);
    }

    @Override
    @Transactional
    public FacultyMonthlySalaryResponse recordSalaryPayment(Long salaryId, FacultyPaymentRequest req, Long actorUserId) {
        FacultyMonthlySalary salary = monthlySalaryRepository.findById(salaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monthly salary not found with id: " + salaryId));

        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than zero.");
        }

        BigDecimal remaining = (salary.getRemainingAmount() != null)
                ? salary.getRemainingAmount()
                : salary.getNetSalary().subtract(salary.getPaidAmount() != null ? salary.getPaidAmount() : BigDecimal.ZERO);

        if (req.getAmount().compareTo(remaining) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Payment amount (₹%.2f) exceeds outstanding balance (₹%.2f).", req.getAmount(), remaining));
        }

        BigDecimal currentPaid = (salary.getPaidAmount() != null) ? salary.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newPaid = currentPaid.add(req.getAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newRemaining = salary.getNetSalary().subtract(newPaid).setScale(2, RoundingMode.HALF_UP);
        if (newRemaining.compareTo(BigDecimal.ZERO) < 0) {
            newRemaining = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        salary.setPaidAmount(newPaid);
        salary.setRemainingAmount(newRemaining);

        if (newRemaining.compareTo(BigDecimal.ZERO) == 0) {
            salary.setPaymentStatus("PAID");
        } else {
            salary.setPaymentStatus("PARTIAL");
        }

        salary.setPaidDate(LocalDate.now());
        if (req.getPaymentMode() != null) salary.setPaymentMode(req.getPaymentMode().toUpperCase());
        if (req.getReferenceNumber() != null) salary.setPaymentReference(req.getReferenceNumber().trim());

        // Generate immutable receipt number: FAC-SAL-{year}-{0001}
        long paymentCount = paymentRepository.count() + 1;
        String receiptNumber = String.format("FAC-SAL-%d-%04d", salary.getYear(), paymentCount);
        while (paymentRepository.existsByReceiptNumber(receiptNumber)) {
            paymentCount++;
            receiptNumber = String.format("FAC-SAL-%d-%04d", salary.getYear(), paymentCount);
        }

        FacultySalaryPayment payment = FacultySalaryPayment.builder()
                .facultyId(salary.getFacultyId())
                .monthlySalaryId(salary.getId())
                .receiptNumber(receiptNumber)
                .amount(req.getAmount())
                .paymentDate(LocalDateTime.now())
                .paymentMode(req.getPaymentMode() != null ? req.getPaymentMode().toUpperCase() : "CASH")
                .referenceNumber(req.getReferenceNumber())
                .remarks(req.getRemarks())
                .recordedBy(actorUserId)
                .build();

        paymentRepository.save(payment);
        FacultyMonthlySalary saved = monthlySalaryRepository.save(salary);

        Faculty faculty = facultyRepository.findById(salary.getFacultyId()).orElse(null);
        return mapToResponse(saved, faculty);
    }

    @Override
    public List<FacultySalaryPaymentResponse> getSalaryPayments(Long monthlySalaryId) {
        return paymentRepository.findByMonthlySalaryIdOrderByPaymentDateAsc(monthlySalaryId).stream()
                .map(this::mapToPaymentResponse).toList();
    }

    @Override
    @Transactional
    public List<FacultyMonthlySalaryResponse> approveAllPayroll(Integer month, Integer year, Long actorUserId) {
        LocalDate now = LocalDate.now();
        int m = (month != null) ? month : now.getMonthValue();
        int y = (year != null) ? year : now.getYear();

        List<FacultyMonthlySalary> list = monthlySalaryRepository.findByMonthAndYear(m, y);
        for (FacultyMonthlySalary sal : list) {
            sal.setPayrollStatus("APPROVED");
            sal.setApprovedBy(actorUserId);
            sal.setApprovedAt(LocalDateTime.now());
            monthlySalaryRepository.save(sal);
        }
        return list.stream().map(sal -> {
            Faculty f = facultyRepository.findById(sal.getFacultyId()).orElse(null);
            return mapToResponse(sal, f);
        }).toList();
    }

    @Override
    @Transactional
    public FacultyMonthlySalaryResponse approvePayroll(Long id, Long actorUserId) {
        FacultyMonthlySalary sal = monthlySalaryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monthly salary not found with id: " + id));

        sal.setPayrollStatus("APPROVED");
        sal.setApprovedBy(actorUserId);
        sal.setApprovedAt(LocalDateTime.now());
        FacultyMonthlySalary saved = monthlySalaryRepository.save(sal);

        Faculty f = facultyRepository.findById(saved.getFacultyId()).orElse(null);
        return mapToResponse(saved, f);
    }

    @Override
    public List<FacultyMonthlySalaryResponse> getSalaryHistory(Long facultyId) {
        Faculty f = facultyRepository.findById(facultyId).orElse(null);
        return monthlySalaryRepository.findByFacultyIdOrderByYearDescMonthDesc(facultyId).stream()
                .map(sal -> mapToResponse(sal, f))
                .toList();
    }

    private FacultyMonthlySalaryResponse mapToResponse(FacultyMonthlySalary s, Faculty f) {
        String name = (f != null) ? f.getName() : "Unknown";
        String code = (f != null && f.getFacultyCode() != null) ? f.getFacultyCode() : (f != null ? String.format("FAC-%04d", f.getId()) : "");
        String dept = (f != null && f.getDepartment() != null) ? f.getDepartment() : "Academic";
        String desig = (f != null && f.getDesignation() != null) ? f.getDesignation() : "";
        String phone = (f != null && f.getPhone() != null) ? f.getPhone() : "";

        return FacultyMonthlySalaryResponse.builder()
                .id(s.getId())
                .facultyId(s.getFacultyId())
                .facultyName(name)
                .facultyCode(code)
                .department(dept)
                .designation(desig)
                .phone(phone)
                .month(s.getMonth())
                .year(s.getYear())
                .workingDays(s.getWorkingDays())
                .presentDays(s.getPresentDays())
                .halfDays(s.getHalfDays())
                .absentDays(s.getAbsentDays())
                .paidLeaveDays(s.getPaidLeaveDays())
                .unpaidLeaveDays(s.getUnpaidLeaveDays())
                .baseSalary(s.getBaseSalary())
                .grossSalary(s.getGrossSalary())
                .attendanceDeduction(s.getAttendanceDeduction())
                .otherDeduction(s.getOtherDeduction())
                .netSalary(s.getNetSalary())
                .paidAmount(s.getPaidAmount())
                .remainingAmount(s.getRemainingAmount())
                .paymentStatus(s.getPaymentStatus())
                .paidDate(s.getPaidDate())
                .paymentMode(s.getPaymentMode())
                .paymentReference(s.getPaymentReference())
                .salaryConfigSnapshot(s.getSalaryConfigSnapshot())
                .remarks(s.getRemarks())
                .payrollStatus(s.getPayrollStatus())
                .approvedBy(s.getApprovedBy())
                .approvedAt(s.getApprovedAt())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private FacultySalaryPaymentResponse mapToPaymentResponse(FacultySalaryPayment p) {
        return FacultySalaryPaymentResponse.builder()
                .id(p.getId())
                .facultyId(p.getFacultyId())
                .monthlySalaryId(p.getMonthlySalaryId())
                .receiptNumber(p.getReceiptNumber())
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate())
                .paymentMode(p.getPaymentMode())
                .referenceNumber(p.getReferenceNumber())
                .remarks(p.getRemarks())
                .recordedBy(p.getRecordedBy())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
