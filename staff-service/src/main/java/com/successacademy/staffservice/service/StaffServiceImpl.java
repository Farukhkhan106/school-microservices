package com.successacademy.staffservice.service;

import com.successacademy.staffservice.client.AuthServiceClient;
import com.successacademy.staffservice.dto.*;
import com.successacademy.staffservice.model.*;
import com.successacademy.staffservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;
    private final StaffShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftRepository shiftRepository;
    private final StaffAttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final StaffSalaryRepository salaryRepository;
    private final AuthServiceClient authServiceClient;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public StaffResponse createStaff(StaffRequest req, Long actorUserId) {
        if (req.getFirstName() == null || req.getFirstName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First name is required.");
        }
        if (req.getLastName() == null || req.getLastName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Last name is required.");
        }

        // Validate department if given
        if (req.getDepartmentId() != null && !departmentRepository.existsById(req.getDepartmentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department does not exist with id: " + req.getDepartmentId());
        }

        // Validate designation if given
        if (req.getDesignationId() != null) {
            Designation desig = designationRepository.findById(req.getDesignationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designation does not exist with id: " + req.getDesignationId()));
            if (req.getDepartmentId() != null && !desig.getDepartmentId().equals(req.getDepartmentId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designation does not belong to the selected department.");
            }
        }

        String staffCode = (req.getEmployeeId() != null && !req.getEmployeeId().isBlank())
                ? req.getEmployeeId().trim().toUpperCase()
                : generateUniqueStaffCode();

        if (staffRepository.existsByStaffCode(staffCode)) {
            if (req.getEmployeeId() != null && !req.getEmployeeId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee ID already exists: " + staffCode);
            }
            staffCode = generateUniqueStaffCode();
        }

        Staff staff = Staff.builder()
                .staffCode(staffCode)
                .firstName(req.getFirstName().trim())
                .middleName(req.getMiddleName() != null ? req.getMiddleName().trim() : null)
                .lastName(req.getLastName().trim())
                .photoUrl(req.getPhotoUrl())
                .gender(req.getGender())
                .dateOfBirth(req.getDateOfBirth())
                .phone(req.getPhone() != null && !req.getPhone().isBlank() ? req.getPhone().trim() : null)
                .alternatePhone(req.getAlternatePhone())
                .email(req.getEmail() != null && !req.getEmail().isBlank() ? req.getEmail().trim() : null)
                .address(req.getAddress())
                .city(req.getCity())
                .state(req.getState())
                .pincode(req.getPincode())
                .emergencyContactName(req.getEmergencyContactName())
                .emergencyContactRelation(req.getEmergencyContactRelation())
                .emergencyContactPhone(req.getEmergencyContactPhone())
                .departmentId(req.getDepartmentId())
                .department(req.getDepartment())
                .designationId(req.getDesignationId())
                .designation(req.getDesignation())
                .category(req.getCategory() != null && !req.getCategory().isBlank() ? req.getCategory().trim() : "Other")
                .employmentType(req.getEmploymentType() != null ? req.getEmploymentType() : "FULL_TIME")
                .joiningDate(req.getJoiningDate() != null ? req.getJoiningDate() : LocalDate.now())
                .lastWorkingDate(req.getLastWorkingDate())
                .reportingManagerStaffId(req.getReportingManagerStaffId())
                .status(req.getStatus() != null ? req.getStatus() : "ACTIVE")
                .systemAccessStatus("NOT_PROVISIONED")
                .accessProfile(req.getAccessProfile())
                .build();

        Staff saved = staffRepository.save(staff);

        // Optional initial salary configuration
        if (req.getBaseSalary() != null) {
            StaffSalary salary = StaffSalary.builder()
                    .staffId(saved.getId())
                    .salaryType(req.getSalaryType() != null && !req.getSalaryType().isBlank() ? req.getSalaryType().trim().toUpperCase() : "MONTHLY")
                    .baseSalary(req.getBaseSalary())
                    .monthlySalary(req.getBaseSalary())
                    .basicSalary(req.getBaseSalary())
                    .transportAllowance(req.getTransportAllowance() != null ? req.getTransportAllowance() : java.math.BigDecimal.ZERO)
                    .otherAllowance(req.getOtherAllowance() != null ? req.getOtherAllowance() : java.math.BigDecimal.ZERO)
                    .fixedDeduction(req.getFixedDeduction() != null ? req.getFixedDeduction() : java.math.BigDecimal.ZERO)
                    .effectiveFrom(saved.getJoiningDate() != null ? saved.getJoiningDate() : LocalDate.now())
                    .active(true)
                    .build();
            salaryRepository.save(salary);
        }

        // Optional System Access Provisioning
        if (Boolean.TRUE.equals(req.getCreateSystemAccess())) {
            String uname = (req.getUsername() != null && !req.getUsername().isBlank())
                    ? req.getUsername().trim()
                    : (saved.getFirstName() + "." + saved.getLastName()).toLowerCase().replaceAll("[^a-z0-9.]", "");
            String pass = (req.getPassword() != null && !req.getPassword().isBlank())
                    ? req.getPassword()
                    : "STF" + String.format("%04d", saved.getId());

            Long authUserId = authServiceClient.createStaffUser(saved.getId(), uname, pass, saved.getEmail());
            if (authUserId != null) {
                saved.setUserId(authUserId);
                saved.setSystemAccessStatus("ACTIVE");
                saved = staffRepository.save(saved);
                auditLogService.log(actorUserId, saved.getId(), "LOGIN_ENABLED", null, uname, "Initial login provisioning");
            }
        }

        auditLogService.log(actorUserId, saved.getId(), "STAFF_CREATED", null, saved.getStaffCode(),
                "Created staff " + saved.getFirstName() + " " + saved.getLastName());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public StaffResponse updateStaff(Long id, StaffRequest req, Long actorUserId) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + id));

        if (req.getFirstName() != null && !req.getFirstName().isBlank()) staff.setFirstName(req.getFirstName().trim());
        if (req.getMiddleName() != null) staff.setMiddleName(req.getMiddleName().trim());
        if (req.getLastName() != null && !req.getLastName().isBlank()) staff.setLastName(req.getLastName().trim());
        if (req.getGender() != null) staff.setGender(req.getGender());
        if (req.getDateOfBirth() != null) staff.setDateOfBirth(req.getDateOfBirth());
        if (req.getPhotoUrl() != null) staff.setPhotoUrl(req.getPhotoUrl());

        // Contact info — can be updated or cleared
        staff.setPhone(req.getPhone() != null && !req.getPhone().isBlank() ? req.getPhone().trim() : null);
        staff.setAlternatePhone(req.getAlternatePhone());
        staff.setEmail(req.getEmail() != null && !req.getEmail().isBlank() ? req.getEmail().trim() : null);

        // Address
        staff.setAddress(req.getAddress());
        staff.setCity(req.getCity());
        staff.setState(req.getState());
        staff.setPincode(req.getPincode());

        // Emergency Contact
        staff.setEmergencyContactName(req.getEmergencyContactName());
        staff.setEmergencyContactRelation(req.getEmergencyContactRelation());
        staff.setEmergencyContactPhone(req.getEmergencyContactPhone());

        // Org updates
        if (req.getDepartmentId() != null) {
            if (!departmentRepository.existsById(req.getDepartmentId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department does not exist.");
            }
            staff.setDepartmentId(req.getDepartmentId());
        }

        if (req.getDesignationId() != null) {
            Designation desig = designationRepository.findById(req.getDesignationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designation does not exist."));
            if (staff.getDepartmentId() != null && !desig.getDepartmentId().equals(staff.getDepartmentId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Designation does not belong to staff department.");
            }
            staff.setDesignationId(req.getDesignationId());
        }

        if (req.getCategory() != null && !req.getCategory().isBlank()) staff.setCategory(req.getCategory().trim());
        if (req.getDesignation() != null) staff.setDesignation(req.getDesignation().trim());
        if (req.getDepartment() != null) staff.setDepartment(req.getDepartment().trim());

        if (req.getEmploymentType() != null) staff.setEmploymentType(req.getEmploymentType());
        if (req.getJoiningDate() != null) staff.setJoiningDate(req.getJoiningDate());
        if (req.getLastWorkingDate() != null) staff.setLastWorkingDate(req.getLastWorkingDate());

        // Cycle check on reporting manager
        if (req.getReportingManagerStaffId() != null) {
            if (req.getReportingManagerStaffId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff cannot be their own reporting manager.");
            }
            staff.setReportingManagerStaffId(req.getReportingManagerStaffId());
        }

        if (req.getAccessProfile() != null) staff.setAccessProfile(req.getAccessProfile());

        if (req.getBaseSalary() != null) {
            StaffSalary salary = salaryRepository.findByStaffId(staff.getId())
                    .orElseGet(() -> StaffSalary.builder().staffId(staff.getId()).build());
            if (req.getSalaryType() != null && !req.getSalaryType().isBlank()) {
                salary.setSalaryType(req.getSalaryType().trim().toUpperCase());
            }
            salary.setBaseSalary(req.getBaseSalary());
            salary.setMonthlySalary(req.getBaseSalary());
            salary.setBasicSalary(req.getBaseSalary());
            if (req.getTransportAllowance() != null) salary.setTransportAllowance(req.getTransportAllowance());
            if (req.getOtherAllowance() != null) salary.setOtherAllowance(req.getOtherAllowance());
            if (req.getFixedDeduction() != null) salary.setFixedDeduction(req.getFixedDeduction());
            salary.setActive(true);
            salaryRepository.save(salary);
        }

        Staff updated = staffRepository.save(staff);
        auditLogService.log(actorUserId, updated.getId(), "STAFF_UPDATED", null, null, "Updated staff profile");
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public StaffResponse updateStatus(Long id, StaffStatusUpdateRequest req, Long actorUserId) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + id));

        String newStatus = req.getStatus() != null ? req.getStatus().trim().toUpperCase() : "ACTIVE";
        String oldStatus = staff.getStatus();
        staff.setStatus(newStatus);

        if (req.getLastWorkingDate() != null) {
            staff.setLastWorkingDate(req.getLastWorkingDate());
        } else if ("RESIGNED".equals(newStatus) || "TERMINATED".equals(newStatus) || "RETIRED".equals(newStatus)) {
            if (staff.getLastWorkingDate() == null) {
                staff.setLastWorkingDate(LocalDate.now());
            }
        }

        // Disable login access if leaving the school
        if ("RESIGNED".equals(newStatus) || "TERMINATED".equals(newStatus) || "RETIRED".equals(newStatus) || "SUSPENDED".equals(newStatus)) {
            if ("ACTIVE".equals(staff.getSystemAccessStatus())) {
                staff.setSystemAccessStatus("DISABLED");
            }
        } else if ("ACTIVE".equals(newStatus) && "DISABLED".equals(staff.getSystemAccessStatus()) && staff.getUserId() != null) {
            staff.setSystemAccessStatus("ACTIVE");
        }

        Staff updated = staffRepository.save(staff);
        auditLogService.log(actorUserId, staff.getId(), "STATUS_CHANGED", oldStatus, newStatus, req.getReason());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public StaffResponse provisionAccess(Long id, StaffProvisionRequest req, Long actorUserId) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + id));

        if (staff.getUserId() != null && "ACTIVE".equals(staff.getSystemAccessStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "System login is already active for this staff member.");
        }

        String uname = (req.getUsername() != null && !req.getUsername().isBlank())
                ? req.getUsername().trim()
                : (staff.getFirstName() + "." + staff.getLastName()).toLowerCase().replaceAll("[^a-z0-9.]", "");
        String pass = (req.getPassword() != null && !req.getPassword().isBlank())
                ? req.getPassword()
                : "STF" + String.format("%04d", staff.getId());

        Long authUserId = authServiceClient.createStaffUser(staff.getId(), uname, pass, staff.getEmail());
        if (authUserId == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Auth service was unable to provision account. Check auth-service status.");
        }

        staff.setUserId(authUserId);
        staff.setSystemAccessStatus("ACTIVE");
        if (req.getAccessProfile() != null && !req.getAccessProfile().isBlank()) {
            staff.setAccessProfile(req.getAccessProfile());
        } else if (staff.getAccessProfile() == null) {
            staff.setAccessProfile("STAFF_BASIC");
        }

        Staff updated = staffRepository.save(staff);
        auditLogService.log(actorUserId, staff.getId(), "LOGIN_ENABLED", null, uname, "Provisioned system access");
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public StaffResponse disableAccess(Long id, Long actorUserId) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + id));

        String oldAccess = staff.getSystemAccessStatus();
        staff.setSystemAccessStatus("DISABLED");
        Staff updated = staffRepository.save(staff);
        auditLogService.log(actorUserId, staff.getId(), "LOGIN_DISABLED", oldAccess, "DISABLED", "Disabled system access");
        return mapToResponse(updated);
    }

    @Override
    public StaffResponse getStaffById(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + id));
        return mapToResponse(staff);
    }

    @Override
    public StaffResponse getStaffByUserId(Long userId) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No staff profile found for user id: " + userId));
        return mapToResponse(staff);
    }

    @Override
    public StaffResponse getStaffByCode(String staffCode) {
        Staff staff = staffRepository.findByStaffCode(staffCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with code: " + staffCode));
        return mapToResponse(staff);
    }

    @Override
    public List<StaffResponse> searchStaff(
            String keyword,
            String category,
            Long departmentId,
            Long designationId,
            String employmentType,
            String status,
            String accessStatus
    ) {
        return staffRepository.searchStaff(
                (keyword != null && !keyword.isBlank()) ? keyword.trim() : null,
                category,
                departmentId,
                designationId,
                employmentType,
                status,
                accessStatus
        ).stream().map(this::mapToResponse).toList();
    }

    @Override
    public String uploadPhoto(Long id, MultipartFile file, Long actorUserId) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with id: " + id));

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required.");
        }

        String savedPath = savePhotoFile(file, "staff_" + id);
        staff.setPhotoUrl(savedPath);
        staffRepository.save(staff);
        auditLogService.log(actorUserId, id, "PHOTO_UPLOADED", null, savedPath, "Updated profile photo");
        return savedPath;
    }

    @Override
    public StaffStatsResponse getStats() {
        LocalDate today = LocalDate.now();
        long total = staffRepository.count();
        long active = staffRepository.countByStatus("ACTIVE");
        long onLeave = leaveRequestRepository.countApprovedOnLeaveForDate(today);
        long withLogin = staffRepository.countBySystemAccessStatus("ACTIVE");
        long withoutLogin = staffRepository.countBySystemAccessStatus("NOT_PROVISIONED");
        long present = attendanceRepository.countByAttendanceDateAndStatus(today, "PRESENT");
        long absent = attendanceRepository.countByAttendanceDateAndStatus(today, "ABSENT");
        long late = attendanceRepository.countByAttendanceDateAndStatus(today, "LATE");

        return StaffStatsResponse.builder()
                .totalStaff(total)
                .activeStaff(active)
                .onLeaveStaff(onLeave)
                .withLoginCount(withLogin)
                .withoutLoginCount(withoutLogin)
                .presentToday(present)
                .absentToday(absent)
                .lateToday(late)
                .build();
    }

    @Override
    public String getNextStaffCode() {
        return generateUniqueStaffCode();
    }

    private synchronized String generateUniqueStaffCode() {
        long maxNum = 0;
        List<Staff> all = staffRepository.findAll();
        for (Staff s : all) {
            if (s.getStaffCode() != null && s.getStaffCode().startsWith("STF-")) {
                try {
                    long n = Long.parseLong(s.getStaffCode().substring(4));
                    if (n > maxNum) maxNum = n;
                } catch (Exception ignored) {}
            }
        }
        long nextNum = maxNum + 1;
        String candidate = String.format("STF-%04d", nextNum);
        while (staffRepository.existsByStaffCode(candidate)) {
            nextNum++;
            candidate = String.format("STF-%04d", nextNum);
        }
        return candidate;
    }

    private StaffResponse mapToResponse(Staff s) {
        String deptName = "";
        String deptCode = "";
        if (s.getDepartmentId() != null) {
            Optional<Department> dOpt = departmentRepository.findById(s.getDepartmentId());
            if (dOpt.isPresent()) {
                deptName = dOpt.get().getName();
                deptCode = dOpt.get().getCode();
            }
        }

        String desigName = "";
        String desigCode = "";
        if (s.getDesignationId() != null) {
            Optional<Designation> desigOpt = designationRepository.findById(s.getDesignationId());
            if (desigOpt.isPresent()) {
                desigName = desigOpt.get().getName();
                desigCode = desigOpt.get().getCode();
            }
        }

        String managerName = "";
        if (s.getReportingManagerStaffId() != null) {
            managerName = staffRepository.findById(s.getReportingManagerStaffId())
                    .map(m -> (m.getFirstName() + " " + m.getLastName()).trim())
                    .orElse("");
        }

        // Active shift
        Long currentShiftId = null;
        String currentShiftName = null;
        Optional<StaffShiftAssignment> activeShift = shiftAssignmentRepository.findActiveShiftForDate(s.getId(), LocalDate.now());
        if (activeShift.isPresent()) {
            currentShiftId = activeShift.get().getShiftId();
            currentShiftName = shiftRepository.findById(currentShiftId).map(Shift::getName).orElse(null);
        }

        String middle = (s.getMiddleName() != null && !s.getMiddleName().isBlank()) ? s.getMiddleName().trim() + " " : "";
        String fullName = (s.getFirstName().trim() + " " + middle + s.getLastName().trim()).replaceAll("\\s+", " ");

        String finalDept = (deptName != null && !deptName.isBlank()) ? deptName : s.getDepartment();
        String finalDesig = (desigName != null && !desigName.isBlank()) ? desigName : s.getDesignation();

        // Salary info
        String salType = "MONTHLY";
        java.math.BigDecimal baseSal = java.math.BigDecimal.ZERO;
        java.math.BigDecimal grossSal = java.math.BigDecimal.ZERO;
        java.math.BigDecimal netSal = java.math.BigDecimal.ZERO;

        Optional<StaffSalary> salOpt = (salaryRepository != null && s.getId() != null)
                ? salaryRepository.findByStaffId(s.getId())
                : Optional.empty();
        if (salOpt.isPresent()) {
            StaffSalary sal = salOpt.get();
            salType = sal.getSalaryType() != null ? sal.getSalaryType() : "MONTHLY";
            baseSal = sal.getBaseSalary() != null ? sal.getBaseSalary() : (sal.getMonthlySalary() != null ? sal.getMonthlySalary() : java.math.BigDecimal.ZERO);
            java.math.BigDecimal trans = sal.getTransportAllowance() != null ? sal.getTransportAllowance() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal oth = sal.getOtherAllowance() != null ? sal.getOtherAllowance() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal ded = sal.getFixedDeduction() != null ? sal.getFixedDeduction() : java.math.BigDecimal.ZERO;
            grossSal = baseSal.add(trans).add(oth);
            netSal = grossSal.subtract(ded);
            if (netSal.compareTo(java.math.BigDecimal.ZERO) < 0) netSal = java.math.BigDecimal.ZERO;
        }

        return StaffResponse.builder()
                .id(s.getId())
                .staffCode(s.getStaffCode())
                .firstName(s.getFirstName())
                .middleName(s.getMiddleName())
                .lastName(s.getLastName())
                .fullName(fullName)
                .photoUrl(s.getPhotoUrl())
                .gender(s.getGender())
                .dateOfBirth(s.getDateOfBirth())
                .phone(s.getPhone())
                .alternatePhone(s.getAlternatePhone())
                .email(s.getEmail())
                .address(s.getAddress())
                .city(s.getCity())
                .state(s.getState())
                .pincode(s.getPincode())
                .emergencyContactName(s.getEmergencyContactName())
                .emergencyContactRelation(s.getEmergencyContactRelation())
                .emergencyContactPhone(s.getEmergencyContactPhone())
                .departmentId(s.getDepartmentId())
                .departmentName(deptName)
                .departmentCode(deptCode)
                .department(finalDept)
                .designationId(s.getDesignationId())
                .designationName(desigName)
                .designationCode(desigCode)
                .designation(finalDesig)
                .category(s.getCategory() != null ? s.getCategory() : "Other")
                .salaryType(salType)
                .baseSalary(baseSal)
                .grossSalary(grossSal)
                .netSalary(netSal)
                .employmentType(s.getEmploymentType())
                .joiningDate(s.getJoiningDate())
                .lastWorkingDate(s.getLastWorkingDate())
                .reportingManagerStaffId(s.getReportingManagerStaffId())
                .reportingManagerName(managerName)
                .status(s.getStatus())
                .systemAccessStatus(s.getSystemAccessStatus())
                .userId(s.getUserId())
                .accessProfile(s.getAccessProfile())
                .currentShiftId(currentShiftId)
                .currentShiftName(currentShiftName)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private String savePhotoFile(MultipartFile file, String prefix) {
        String originalName = file.getOriginalFilename();
        String ext = ".jpg";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }

        try {
            Path uploadDir = Paths.get("uploads", "staff", "photos").toAbsolutePath().normalize();
            File dir = uploadDir.toFile();
            if (!dir.exists()) dir.mkdirs();

            String filename = prefix + "_" + System.currentTimeMillis() + ext;
            Path targetLocation = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/staff-service/uploads/staff/photos/" + filename;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store photo: " + e.getMessage());
        }
    }
}
