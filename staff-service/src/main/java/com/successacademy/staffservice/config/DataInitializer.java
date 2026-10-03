package com.successacademy.staffservice.config;

import com.successacademy.staffservice.model.*;
import com.successacademy.staffservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final ShiftRepository shiftRepository;
    private final StaffRepository staffRepository;
    private final StaffAttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final StaffShiftAssignmentRepository shiftAssignmentRepository;
    private final StaffSalaryRepository salaryRepository;
    private final StaffMonthlySalaryRepository monthlySalaryRepository;
    private final StaffSalaryPaymentRepository paymentRepository;
    private final StaffDocumentRepository documentRepository;
    private final StaffAuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking Staff Service master reference data and staff roster...");

        // ── 1. Departments (Idempotent: get or create) ───────────────
        Department acaDept = getOrCreateDepartment("ACADEMICS", "Academics & Teaching", "Teaching faculty, curriculum delivery, and student tutoring");
        Department adminDept = getOrCreateDepartment("ADMINISTRATION", "Administration & Office", "School administrative, front office, and clerical operations");
        Department hkDept = getOrCreateDepartment("HOUSEKEEPING", "Housekeeping & Sanitation", "Campus cleanliness, sanitation, and hygiene upkeep");
        Department secDept = getOrCreateDepartment("SECURITY", "Security & Safety", "Campus entrance guarding, perimeter monitoring, and student safety");
        Department transDept = getOrCreateDepartment("TRANSPORT", "Transport & Fleet", "School bus fleet management, driver logistics, and student commute");
        Department accDept = getOrCreateDepartment("ACCOUNTS", "Accounts & Finance", "School billing, fee collection receipts, and procurement accounting");
        Department libDept = getOrCreateDepartment("LIBRARY", "Library", "Book issue, cataloguing, and reading room management");
        Department maintDept = getOrCreateDepartment("MAINTENANCE", "Maintenance & Electrical", "Campus electrical, plumbing, carpentry, and infrastructure maintenance");

        // ── 2. Designations (Idempotent: get or create) ──────────────
        Designation mathTeacher = getOrCreateDesignation(acaDept.getId(), "MATH_TEACHER", "Senior Mathematics Teacher", "High school mathematics instruction");
        Designation scienceTeacher = getOrCreateDesignation(acaDept.getId(), "SCIENCE_TEACHER", "Senior Science Teacher", "Physics and chemistry science classes");
        Designation englishTeacher = getOrCreateDesignation(acaDept.getId(), "ENGLISH_TEACHER", "English Literature Teacher", "English grammar and language skills");
        Designation hindiTeacher = getOrCreateDesignation(acaDept.getId(), "HINDI_TEACHER", "Hindi Language Teacher", "Hindi literature and poetry");
        Designation compTeacher = getOrCreateDesignation(acaDept.getId(), "COMP_TEACHER", "Computer Science Teacher", "IT lab and computer programming");
        Designation peTeacher = getOrCreateDesignation(acaDept.getId(), "PE_TEACHER", "Physical Education Instructor", "Sports, athletics, and yoga trainer");
        Designation artTeacher = getOrCreateDesignation(acaDept.getId(), "ART_TEACHER", "Art & Craft Instructor", "Drawing, painting, and craftwork");

        Designation cleanerDesig = getOrCreateDesignation(hkDept.getId(), "CLEANER", "Cleaner", "Campus classroom and premises cleaner");
        Designation peonDesig = getOrCreateDesignation(hkDept.getId(), "PEON", "Peon / Office Attendant", "School office errand and bell assistant");
        Designation gardenerDesig = getOrCreateDesignation(hkDept.getId(), "GARDENER", "Gardener / Groundskeeper", "Campus lawn, trees and garden upkeep");

        Designation guardDesig = getOrCreateDesignation(secDept.getId(), "GUARD", "Security Guard", "Gate entry and perimeter security guard");
        Designation headGuardDesig = getOrCreateDesignation(secDept.getId(), "HEAD_GUARD", "Head Security Guard", "Lead security officer and shift incharge");
        Designation cctvOperatorDesig = getOrCreateDesignation(secDept.getId(), "CCTV_OPERATOR", "CCTV Surveillance Operator", "Security camera monitoring");

        Designation driverDesig = getOrCreateDesignation(transDept.getId(), "DRIVER", "School Bus Driver", "Licensed commercial driver for school buses");
        Designation conductorDesig = getOrCreateDesignation(transDept.getId(), "CONDUCTOR", "Bus Conductor / Attendant", "Student safety on bus routes");
        Designation fleetCoordDesig = getOrCreateDesignation(transDept.getId(), "FLEET_COORD", "Fleet & Transport Coordinator", "Bus route planning and fleet supervisor");

        Designation recDesig = getOrCreateDesignation(adminDept.getId(), "RECEPTIONIST", "Front Desk Receptionist", "Reception and parent enquiry handling");
        Designation clerkDesig = getOrCreateDesignation(adminDept.getId(), "OFFICE_CLERK", "Senior Office Clerk", "Student admissions registration and administrative records");

        Designation accountantDesig = getOrCreateDesignation(accDept.getId(), "ACCOUNTANT", "Senior Accountant", "Fee cashier and balance ledger accountant");
        Designation cashierDesig = getOrCreateDesignation(accDept.getId(), "CASHIER", "Fee Cashier", "Counter fee collections and parent receipting");

        Designation librarianDesig = getOrCreateDesignation(libDept.getId(), "LIBRARIAN", "Chief Librarian", "School central library management");
        Designation libAssistantDesig = getOrCreateDesignation(libDept.getId(), "LIB_ASSISTANT", "Library Assistant", "Book issuing and stack shelving");

        Designation electricianDesig = getOrCreateDesignation(maintDept.getId(), "ELECTRICIAN", "Campus Electrician", "Electrical distribution and power maintenance");
        Designation plumberDesig = getOrCreateDesignation(maintDept.getId(), "PLUMBER", "Campus Plumber", "RO plant, water supply and plumbing");

        // ── 3. Shifts (Idempotent: get or create) ────────────────────
        Shift morningShift = getOrCreateShift("Morning Shift", LocalTime.of(6, 30), LocalTime.of(14, 30));
        Shift generalShift = getOrCreateShift("General Shift", LocalTime.of(8, 0), LocalTime.of(16, 30));
        Shift eveningShift = getOrCreateShift("Evening Shift", LocalTime.of(13, 30), LocalTime.of(21, 30));

        // ── 4. Staff Transactional Data Protection ───────────────────
        if (staffRepository.count() > 0) {
            log.info("Staff records already exist in database ({} records). Skipping transactional seed.", staffRepository.count());
            return;
        }

        // ── 5. Initial Baseline Staff Seeding (Only on Empty Database) ──
        // Format: [First, Last, Gender, Phone, Category, Designation, Dept, BaseSalary, DeptId, DesigId]
        Object[][] staffRaw = {
                // Administration & Accounts (Non-Teaching)
                {"Neha", "Saxena", "Female", "9826110011", "Receptionist", "Front Desk Receptionist", "Administration", 22000, adminDept.getId(), recDesig.getId()},
                {"Sanjay", "Rathore", "Male", "9826110012", "Office Staff", "Senior Office Clerk", "Administration", 24000, adminDept.getId(), clerkDesig.getId()},
                {"Preeti", "Nigam", "Female", "9826110013", "Office Staff", "Admissions Coordinator", "Administration", 23000, adminDept.getId(), clerkDesig.getId()},
                {"Dinesh", "Agarwal", "Male", "9826110014", "Accountant", "Senior Accountant", "Accounts", 38000, accDept.getId(), accountantDesig.getId()},
                {"Meena", "Shukla", "Female", "9826110015", "Accountant", "Fee Cashier", "Accounts", 24000, accDept.getId(), cashierDesig.getId()},
                {"Gaurav", "Soni", "Male", "9826110016", "Accountant", "Assistant Accountant", "Accounts", 20000, accDept.getId(), cashierDesig.getId()},
                {"Shalini", "Pandey", "Female", "9826110017", "Librarian", "Chief Librarian", "Library", 28000, libDept.getId(), librarianDesig.getId()},
                {"Ajay", "Yadav", "Male", "9826110018", "Librarian", "Library Assistant", "Library", 17000, libDept.getId(), libAssistantDesig.getId()},

                // Transport - Bus Drivers & Conductors (19-30)
                {"Ramesh", "Solanki", "Male", "9826110019", "Driver", "School Bus Driver (Route 1)", "Transport", 19000, transDept.getId(), driverDesig.getId()},
                {"Gopal", "Singh", "Male", "9826110020", "Driver", "School Bus Driver (Route 2)", "Transport", 19000, transDept.getId(), driverDesig.getId()},
                {"Mahesh", "Pawar", "Male", "9826110021", "Driver", "School Bus Driver (Route 3)", "Transport", 19000, transDept.getId(), driverDesig.getId()},
                {"Kailash", "Chouhan", "Male", "9826110022", "Driver", "School Bus Driver (Route 4)", "Transport", 18500, transDept.getId(), driverDesig.getId()},
                {"Bhagwan", "Das", "Male", "9826110023", "Driver", "School Bus Driver (Route 5)", "Transport", 18500, transDept.getId(), driverDesig.getId()},
                {"Santosh", "Patidar", "Male", "9826110024", "Driver", "School Bus Driver (Route 6)", "Transport", 19500, transDept.getId(), driverDesig.getId()},
                {"Harish", "Solanki", "Male", "9826110025", "Driver", "School Bus Driver (Route 7)", "Transport", 18000, transDept.getId(), driverDesig.getId()},
                {"Satish", "Verma", "Male", "9826110026", "Driver", "Fleet & Transport Coordinator", "Transport", 26000, transDept.getId(), fleetCoordDesig.getId()},
                {"Babu", "Lal", "Male", "9826110027", "Driver", "Bus Conductor / Attendant", "Transport", 14000, transDept.getId(), conductorDesig.getId()},
                {"Mohan", "Koli", "Male", "9826110028", "Driver", "Bus Conductor / Attendant", "Transport", 14000, transDept.getId(), conductorDesig.getId()},
                {"Vinod", "Mewada", "Male", "9826110029", "Driver", "Bus Conductor / Attendant", "Transport", 14000, transDept.getId(), conductorDesig.getId()},
                {"Mukesh", "Bheel", "Male", "9826110030", "Driver", "Bus Conductor / Attendant", "Transport", 13500, transDept.getId(), conductorDesig.getId()},

                // Security Guards (31-38)
                {"Suraj", "Bhan", "Male", "9826110031", "Security", "Head Security Guard", "Security", 21000, secDept.getId(), headGuardDesig.getId()},
                {"Ramcharan", "Yadav", "Male", "9826110032", "Security", "Main Gate Security Guard", "Security", 16000, secDept.getId(), guardDesig.getId()},
                {"Dharmendra", "Singh", "Male", "9826110033", "Security", "Secondary Gate Guard", "Security", 15500, secDept.getId(), guardDesig.getId()},
                {"Jagdish", "Sharma", "Male", "9826110034", "Security", "Campus Night Watchman", "Security", 16500, secDept.getId(), guardDesig.getId()},
                {"Shyamlal", "Chouhan", "Male", "9826110035", "Security", "Perimeter Security Guard", "Security", 15000, secDept.getId(), guardDesig.getId()},
                {"Prakash", "Gond", "Male", "9826110036", "Security", "CCTV Surveillance Operator", "Security", 18000, secDept.getId(), cctvOperatorDesig.getId()},
                {"Kishore", "Malviya", "Male", "9826110037", "Security", "Day Shift Guard", "Security", 15000, secDept.getId(), guardDesig.getId()},
                {"Balram", "Verma", "Male", "9826110038", "Security", "Night Shift Guard", "Security", 15500, secDept.getId(), guardDesig.getId()},

                // Housekeeping & Peons (39-48)
                {"Sundari", "Bai", "Female", "9826110039", "Cleaner", "Senior Floor Cleaner", "Housekeeping", 13000, hkDept.getId(), cleanerDesig.getId()},
                {"Kamla", "Devi", "Female", "9826110040", "Cleaner", "Campus Sanitation Worker", "Housekeeping", 12500, hkDept.getId(), cleanerDesig.getId()},
                {"Shanti", "Malviya", "Female", "9826110041", "Cleaner", "Restroom Sanitation Worker", "Housekeeping", 12500, hkDept.getId(), cleanerDesig.getId()},
                {"Ganga", "Ram", "Male", "9826110042", "Cleaner", "Classroom Cleaner", "Housekeeping", 12000, hkDept.getId(), cleanerDesig.getId()},
                {"Mangilal", "Solanki", "Male", "9826110043", "Peon", "Head Office Peon", "Housekeeping", 15000, hkDept.getId(), peonDesig.getId()},
                {"Bablu", "Chouhan", "Male", "9826110044", "Peon", "Staff Room Attendant", "Housekeeping", 14000, hkDept.getId(), peonDesig.getId()},
                {"Girdhari", "Lal", "Male", "9826110045", "Peon", "Administrative Errand Peon", "Housekeeping", 13500, hkDept.getId(), peonDesig.getId()},
                {"Ramavtar", "Saini", "Male", "9826110046", "Peon", "Campus Groundskeeper / Gardener", "Housekeeping", 14500, hkDept.getId(), gardenerDesig.getId()},
                {"Champa", "Lal", "Male", "9826110047", "Peon", "Campus Groundskeeper / Gardener", "Housekeeping", 14000, hkDept.getId(), gardenerDesig.getId()},
                {"Radhe", "Shyam", "Male", "9826110048", "Cleaner", "Auditorium & Play Area Cleaner", "Housekeeping", 12000, hkDept.getId(), cleanerDesig.getId()},

                // Maintenance & Technical (49-52)
                {"Pramod", "Kushwaha", "Male", "9826110049", "Maintenance", "Campus Chief Electrician", "Maintenance", 22000, maintDept.getId(), electricianDesig.getId()},
                {"Mohit", "Choudhary", "Male", "9826110050", "Maintenance", "Campus Plumber & RO Technician", "Maintenance", 18000, maintDept.getId(), plumberDesig.getId()},
                {"Tarun", "Sen", "Male", "9826110051", "Maintenance", "Assistant Electrician", "Maintenance", 16000, maintDept.getId(), electricianDesig.getId()},
                {"Arun", "Bairagi", "Male", "9826110052", "Maintenance", "Carpentry & Furniture Repair", "Maintenance", 16500, maintDept.getId(), plumberDesig.getId()},
        };

        List<Staff> createdStaffList = new ArrayList<>();

        for (int i = 0; i < staffRaw.length; i++) {
            Object[] row = staffRaw[i];
            String code = String.format("STF-%04d", i + 1);
            String first = (String) row[0];
            String last = (String) row[1];
            String gender = (String) row[2];
            String phone = (String) row[3];
            String cat = (String) row[4];
            String desig = (String) row[5];
            String dept = (String) row[6];
            int baseSalary = (int) row[7];
            Long deptId = (Long) row[8];
            Long desigId = (Long) row[9];

            String status = (i == 50) ? "ON_NOTICE" : (i == 51) ? "INACTIVE" : "ACTIVE";

            Staff staff = Staff.builder()
                    .staffCode(code)
                    .firstName(first)
                    .lastName(last)
                    .gender(gender)
                    .phone(phone)
                    .email(first.toLowerCase() + "." + last.toLowerCase() + "@successacademy.edu")
                    .dateOfBirth(LocalDate.of(1982 + (i % 15), 1 + (i % 12), 1 + (i % 27)))
                    .category(cat)
                    .designation(desig)
                    .department(dept)
                    .departmentId(deptId)
                    .designationId(desigId)
                    .employmentType("FULL_TIME")
                    .joiningDate(LocalDate.of(2021 + (i % 4), 1 + (i % 11), 1 + (i % 25)))
                    .status(status)
                    .systemAccessStatus("NOT_PROVISIONED")
                    .address("Ward No. " + ((i % 15) + 1) + ", Satwas Tehsil")
                    .city("Satwas")
                    .state("Madhya Pradesh")
                    .pincode("455459")
                    .emergencyContactName(first + "'s Guardian")
                    .emergencyContactRelation("Family")
                    .emergencyContactPhone("98765" + String.format("%05d", 10000 + i))
                    .photoUrl("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=200&fit=crop&crop=face")
                    .build();

            Staff saved = staffRepository.save(staff);
            createdStaffList.add(saved);

            // Shift Assignment
            Shift assignedShift = (cat.equals("Security")) ? eveningShift : generalShift;
            shiftAssignmentRepository.save(StaffShiftAssignment.builder()
                    .staffId(saved.getId())
                    .shiftId(assignedShift.getId())
                    .effectiveFrom(LocalDate.of(2026, 1, 1))
                    .build());

            // Salary Configuration
            BigDecimal base = BigDecimal.valueOf(baseSalary);
            BigDecimal transport = BigDecimal.valueOf(cat.equals("Driver") ? 1500 : 1000);
            BigDecimal allowance = BigDecimal.valueOf(500);
            BigDecimal deduction = BigDecimal.valueOf(200);

            StaffSalary salaryCfg = StaffSalary.builder()
                    .staffId(saved.getId())
                    .salaryType("MONTHLY")
                    .baseSalary(base)
                    .monthlySalary(base)
                    .transportAllowance(transport)
                    .otherAllowance(allowance)
                    .fixedDeduction(deduction)
                    .active(true)
                    .effectiveFrom(LocalDate.of(2026, 1, 1))
                    .build();
            salaryRepository.save(salaryCfg);

            // ── Seed Daily Attendance for October 2026 (Dates 1st & 2nd) ──
            int presentDaysCount;
            double halfDayCount = 0.0;
            double absentCount = 0.0;

            if (i < 20) {
                // 100% Present on both recorded days
                attendanceRepository.save(StaffAttendance.builder()
                        .staffId(saved.getId())
                        .attendanceDate(LocalDate.of(2026, 10, 1))
                        .status("PRESENT")
                        .checkIn(LocalTime.of(8, 0))
                        .checkOut(LocalTime.of(16, 30))
                        .source("ADMIN")
                        .build());
                attendanceRepository.save(StaffAttendance.builder()
                        .staffId(saved.getId())
                        .attendanceDate(LocalDate.of(2026, 10, 2))
                        .status("PRESENT")
                        .checkIn(LocalTime.of(8, 5))
                        .checkOut(LocalTime.of(16, 30))
                        .source("ADMIN")
                        .build());
                presentDaysCount = 2;
            } else if (i < 35) {
                // 1 Present, 1 Half Day
                attendanceRepository.save(StaffAttendance.builder()
                        .staffId(saved.getId())
                        .attendanceDate(LocalDate.of(2026, 10, 1))
                        .status("PRESENT")
                        .checkIn(LocalTime.of(8, 0))
                        .checkOut(LocalTime.of(16, 30))
                        .source("ADMIN")
                        .build());
                attendanceRepository.save(StaffAttendance.builder()
                        .staffId(saved.getId())
                        .attendanceDate(LocalDate.of(2026, 10, 2))
                        .status("HALF_DAY")
                        .checkIn(LocalTime.of(8, 0))
                        .checkOut(LocalTime.of(12, 30))
                        .remarks("Half day permission approved")
                        .source("ADMIN")
                        .build());
                presentDaysCount = 1;
                halfDayCount = 1.0;
            } else if (i < 45) {
                // 1 Present, 1 Approved Leave
                attendanceRepository.save(StaffAttendance.builder()
                        .staffId(saved.getId())
                        .attendanceDate(LocalDate.of(2026, 10, 1))
                        .status("PRESENT")
                        .checkIn(LocalTime.of(8, 0))
                        .checkOut(LocalTime.of(16, 30))
                        .source("ADMIN")
                        .build());
                attendanceRepository.save(StaffAttendance.builder()
                        .staffId(saved.getId())
                        .attendanceDate(LocalDate.of(2026, 10, 2))
                        .status("LEAVE")
                        .remarks("Casual leave")
                        .source("ADMIN")
                        .build());
                presentDaysCount = 1;
            } else {
                // 1 Present, 1 Absent
                attendanceRepository.save(StaffAttendance.builder()
                        .staffId(saved.getId())
                        .attendanceDate(LocalDate.of(2026, 10, 1))
                        .status("PRESENT")
                        .checkIn(LocalTime.of(8, 15))
                        .checkOut(LocalTime.of(16, 30))
                        .source("ADMIN")
                        .build());
                attendanceRepository.save(StaffAttendance.builder()
                        .staffId(saved.getId())
                        .attendanceDate(LocalDate.of(2026, 10, 2))
                        .status("ABSENT")
                        .remarks("Absent without intimation")
                        .source("ADMIN")
                        .build());
                presentDaysCount = 1;
                absentCount = 1.0;
            }

            // ── Calculate and Seed Monthly Salary Record for October 2026 ──
            // ── Calculate and Seed Monthly Salary Record for October 2026 ──
            int workingDays = 26;
            double effectivePresent = (double) presentDaysCount + (halfDayCount * 0.5);
            double paidLeaves = (i >= 35 && i < 45) ? 1.0 : 0.0;
            // Only confirmed absent and half-day unpaid portion are deducted (unmarked days are NOT deducted)
            double deductibleUnpaidDays = absentCount + (halfDayCount * 0.5);
            double totalMarkedDays = (double) presentDaysCount + halfDayCount + (paidLeaves > 0 ? 1 : 0) + absentCount;
            double unmarkedDays = Math.max(0.0, (double) workingDays - totalMarkedDays);

            BigDecimal gross = base.add(transport).add(allowance);
            BigDecimal perDay = base.divide(BigDecimal.valueOf(workingDays), 4, RoundingMode.HALF_UP);
            BigDecimal attendanceDeduction = perDay.multiply(BigDecimal.valueOf(deductibleUnpaidDays)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal net = gross.subtract(attendanceDeduction).subtract(deduction);
            if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;

            String paymentStatus;
            BigDecimal paidAmount;
            LocalDate paidDate = null;
            String payMode = null;
            String payRef = null;
            String remarks = null;
            String payrollStatus = "APPROVED";

            if (i < 20) {
                // CASE 1: FULLY PAID (20 staff)
                paymentStatus = "PAID";
                paidAmount = net;
                paidDate = LocalDate.of(2026, 10, 2);
                payMode = (i % 2 == 0) ? "Bank Transfer" : "Cash";
                payRef = (i % 2 == 0) ? "NEFT-202610" + String.format("%04d", i + 1) : "CASH-REC-" + String.format("%04d", i + 1);
                remarks = "Full monthly salary disbursed";
            } else if (i < 36) {
                // CASE 2: PARTIALLY PAID / ADVANCE (16 staff)
                paymentStatus = "PARTIAL";
                paidAmount = (net.compareTo(BigDecimal.valueOf(10000)) > 0) ? BigDecimal.valueOf(10000) : BigDecimal.valueOf(5000);
                paidDate = LocalDate.of(2026, 10, 1);
                payMode = (i % 2 == 0) ? "UPI" : "Cash";
                payRef = (i % 2 == 0) ? "UPI-TXN-" + String.format("%04d", i + 1) : "VOUCHER-" + String.format("%04d", i + 1);
                remarks = "Mid-month advance payment disbursed on request";
            } else {
                // CASE 3: PENDING (16 staff)
                paymentStatus = "PENDING";
                paidAmount = BigDecimal.ZERO;
                payrollStatus = "CALCULATED";
            }

            StaffMonthlySalary monthlySalary = StaffMonthlySalary.builder()
                    .staffId(saved.getId())
                    .month(10)
                    .year(2026)
                    .workingDays(workingDays)
                    .presentDays(effectivePresent)
                    .paidLeaveDays(paidLeaves)
                    .unpaidLeaveDays(0.0)
                    .absentDays(absentCount)
                    .unmarkedDays(unmarkedDays)
                    .grossSalary(gross)
                    .attendanceDeduction(attendanceDeduction)
                    .otherDeduction(deduction)
                    .netSalary(net)
                    .paidAmount(paidAmount)
                    .paymentStatus(paymentStatus)
                    .payrollStatus(payrollStatus)
                    .paidDate(paidDate)
                    .paymentMode(payMode)
                    .paymentReference(payRef)
                    .remarks(remarks)
                    .salaryConfigSnapshot(String.format("{\"baseSalary\":%s,\"transportAllowance\":%s,\"otherAllowance\":%s,\"fixedDeduction\":%s,\"workingDays\":26}",
                            base, transport, allowance, deduction))
                    .build();

            StaffMonthlySalary savedMonthly = monthlySalaryRepository.save(monthlySalary);

            // Record into immutable payment ledger if payment was made
            if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
                StaffSalaryPayment payment = StaffSalaryPayment.builder()
                        .monthlySalaryId(savedMonthly.getId())
                        .staffId(saved.getId())
                        .amount(paidAmount)
                        .paymentDate(paidDate != null ? paidDate : LocalDate.now())
                        .paymentMode(payMode != null ? payMode : "Cash")
                        .referenceNumber(payRef)
                        .receiptNumber(String.format("SAL-2026-%04d", i + 1))
                        .remarks(remarks)
                        .createdBy(1L)
                        .build();
                paymentRepository.save(payment);
            }
        }

        log.info("✅ Created {} fresh staff members with active salaries, attendance, and all payment cases (PAID, PARTIAL, PENDING)!", createdStaffList.size());

        // ── 5. Seed Leave Queue ─────────────────────────────────────
        seedSampleLeaves(createdStaffList);
    }

    private void seedSampleLeaves(List<Staff> staffList) {
        if (staffList.size() < 10) return;

        List<LeaveRequest> leaves = new ArrayList<>();

        leaves.add(LeaveRequest.builder()
                .staffId(staffList.get(0).getId())
                .leaveType("CASUAL")
                .fromDate(LocalDate.of(2026, 10, 5))
                .toDate(LocalDate.of(2026, 10, 6))
                .reason("Attending family wedding ceremony")
                .status("APPROVED")
                .build());

        leaves.add(LeaveRequest.builder()
                .staffId(staffList.get(5).getId())
                .leaveType("SICK")
                .fromDate(LocalDate.of(2026, 10, 8))
                .toDate(LocalDate.of(2026, 10, 9))
                .reason("Medical doctor consultation and rest")
                .status("PENDING")
                .build());

        leaves.add(LeaveRequest.builder()
                .staffId(staffList.get(12).getId())
                .leaveType("CASUAL")
                .fromDate(LocalDate.of(2026, 10, 10))
                .toDate(LocalDate.of(2026, 10, 10))
                .reason("Personal urgent bank work")
                .status("PENDING")
                .build());

        leaves.add(LeaveRequest.builder()
                .staffId(staffList.get(20).getId())
                .leaveType("UNPAID")
                .fromDate(LocalDate.of(2026, 10, 12))
                .toDate(LocalDate.of(2026, 10, 14))
                .reason("Out of station travel")
                .status("APPROVED")
                .build());

        leaves.add(LeaveRequest.builder()
                .staffId(staffList.get(25).getId())
                .leaveType("CASUAL")
                .fromDate(LocalDate.of(2026, 10, 15))
                .toDate(LocalDate.of(2026, 10, 16))
                .reason("Festival celebration at native village")
                .status("REJECTED")
                .rejectionReason("Excess bus driver leaves on same route")
                .build());

        leaves.add(LeaveRequest.builder()
                .staffId(staffList.get(32).getId())
                .leaveType("SICK")
                .fromDate(LocalDate.of(2026, 10, 18))
                .toDate(LocalDate.of(2026, 10, 19))
                .reason("Viral fever")
                .status("PENDING")
                .build());

        leaveRequestRepository.saveAll(leaves);
        log.info("Seeded {} sample leave requests in the queue.", leaves.size());
    }

    private Department getOrCreateDepartment(String code, String name, String desc) {
        return departmentRepository.findByCodeIgnoreCase(code)
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .code(code).name(name).description(desc).status("ACTIVE").build()));
    }

    private Designation getOrCreateDesignation(Long deptId, String code, String name, String desc) {
        return designationRepository.findByCodeIgnoreCase(code)
                .orElseGet(() -> designationRepository.save(Designation.builder()
                        .departmentId(deptId).code(code).name(name).description(desc).status("ACTIVE").build()));
    }

    private Shift getOrCreateShift(String name, LocalTime start, LocalTime end) {
        return shiftRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> shiftRepository.save(Shift.builder()
                        .name(name).startTime(start).endTime(end).graceMinutes(15)
                        .workingDays("Mon,Tue,Wed,Thu,Fri,Sat").status("ACTIVE").build()));
    }
}
