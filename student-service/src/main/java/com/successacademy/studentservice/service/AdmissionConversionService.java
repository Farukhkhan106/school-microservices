package com.successacademy.studentservice.service;

import com.successacademy.studentservice.client.AcademicServiceClient;
import com.successacademy.studentservice.client.AuthServiceClient;
import com.successacademy.studentservice.dto.AdmissionConversionRequest;
import com.successacademy.studentservice.dto.StudentResponse;
import com.successacademy.studentservice.model.*;
import com.successacademy.studentservice.repository.*;
import com.successacademy.studentservice.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdmissionConversionService {

    private final AdmissionApplicationRepository applicationRepository;
    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final StudentLifecycleLogRepository lifecycleLogRepository;
    private final AdmissionSequenceService sequenceService;
    private final AcademicServiceClient academicServiceClient;
    private final AuthServiceClient authServiceClient;

    /**
     * Converts an approved admission application to an active student.
     * Idempotent: If already converted, returns the existing student.
     * Transactional: Either all entities (Student, Guardian, Enrollment, Log) commit or rollback.
     */
    @Transactional
    public StudentResponse convertToStudent(Long applicationId, AdmissionConversionRequest request, String actor) {
        String tenantId = TenantContext.getTenantId();

        AdmissionApplication app = applicationRepository.findByIdAndTenantId(applicationId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission application not found"));

        // Idempotency check
        if (app.getStatus() == AdmissionStatus.CONVERTED && app.getStudentId() != null) {
            log.info("Idempotent check: Application {} already converted to student {}. Returning existing student.",
                    applicationId, app.getStudentId());
            Student existingStudent = studentRepository.findByIdAndTenantId(app.getStudentId(), tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Converted student record missing"));
            return mapStudentToResponse(existingStudent);
        }

        if (app.getStatus() != AdmissionStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Application must be in APPROVED state to convert to student. Current status: " + app.getStatus());
        }

        String targetClass = (request != null && request.getStudentClass() != null && !request.getStudentClass().isBlank())
                ? request.getStudentClass().trim()
                : app.getApplyingClass();

        String targetSection = (request != null && request.getSection() != null && !request.getSection().isBlank())
                ? request.getSection().trim().toUpperCase()
                : "A";

        // Assign safe roll number
        String targetRollNo;
        if (request != null && request.getRollNumber() != null && !request.getRollNumber().isBlank()) {
            targetRollNo = request.getRollNumber().trim();
        } else {
            long currentCount = studentRepository.countByTenantIdAndStudentClassAndSection(tenantId, targetClass, targetSection);
            targetRollNo = String.valueOf(currentCount + 1);
        }

        // Generate tenant-scoped unique admission number
        String admissionNo = sequenceService.nextAdmissionNumber(tenantId);

        // Build address string
        StringBuilder addr = new StringBuilder();
        if (app.getAddressLine1() != null) addr.append(app.getAddressLine1());
        if (app.getAddressLine2() != null && !app.getAddressLine2().isBlank()) addr.append(", ").append(app.getAddressLine2());
        if (app.getCity() != null) addr.append(", ").append(app.getCity());
        if (app.getState() != null) addr.append(", ").append(app.getState());
        if (app.getPostalCode() != null) addr.append(" - ").append(app.getPostalCode());

        String fName = (app.getFatherName() != null && !app.getFatherName().isBlank())
                ? app.getFatherName()
                : (app.getGuardianName() != null && !app.getGuardianName().isBlank() ? app.getGuardianName() : "Parent");

        Student student = Student.builder()
                .tenantId(tenantId)
                .firstName(app.getFirstName())
                .lastName(app.getLastName())
                .admissionNo(admissionNo)
                .rollNo(targetRollNo)
                .studentClass(targetClass)
                .section(targetSection)
                .email(app.getPrimaryContactEmail() != null ? app.getPrimaryContactEmail() : admissionNo.toLowerCase() + "@school.local")
                .phone(app.getPrimaryContactPhone())
                .gender(app.getGender())
                .dateOfBirth(app.getDateOfBirth() != null ? app.getDateOfBirth() : LocalDate.of(2010, 1, 1))
                .address(addr.toString())
                .fatherName(fName)
                .status("ACTIVE")
                .lifecycleStatus("ACTIVE")
                .admissionId(app.getId())
                .build();

        Student savedStudent = studentRepository.save(student);

        // 2. Link Guardian if primary contact information is available
        if (app.getPrimaryContactName() != null && !app.getPrimaryContactName().isBlank()) {
            linkPrimaryGuardian(app, savedStudent.getId(), tenantId);
        }

        // 3. Auto-enroll in Academic Service (Phase 2 Session Enrollment)
        academicServiceClient.enrollInActiveSession(
                savedStudent.getId(),
                savedStudent.getFirstName() + " " + savedStudent.getLastName(),
                savedStudent.getAdmissionNo(),
                savedStudent.getStudentClass(),
                savedStudent.getSection(),
                savedStudent.getRollNo(),
                tenantId
        );

        // 4. Optionally create Auth User Account (Phase 6 User Lifecycle)
        if (request != null && Boolean.TRUE.equals(request.getCreateStudentUserAccount())) {
            try {
                authServiceClient.createStudentUser(
                        savedStudent.getId(),
                        savedStudent.getFirstName(),
                        savedStudent.getLastName(),
                        savedStudent.getEmail(),
                        savedStudent.getAdmissionNo()
                );
            } catch (Exception e) {
                log.warn("Non-fatal: Failed to create student auth user: {}", e.getMessage());
            }
        }

        // 5. Update Application Status to CONVERTED
        app.setStatus(AdmissionStatus.CONVERTED);
        app.setStudentId(savedStudent.getId());
        app.setAdmissionNumber(savedStudent.getAdmissionNo());
        app.setConvertedAt(LocalDateTime.now());
        app.setConvertedBy(actor != null ? actor : "system");
        app.setUpdatedBy(actor != null ? actor : "system");
        applicationRepository.save(app);

        // 6. Record Initial Lifecycle Log
        StudentLifecycleLog logEntry = StudentLifecycleLog.builder()
                .studentId(savedStudent.getId())
                .tenantId(tenantId)
                .fromStatus("APPLICANT")
                .toStatus("ACTIVE")
                .action("ADMISSION_CONVERSION")
                .reason("Admission converted from application " + app.getApplicationNumber())
                .notes("Admitted into Class " + targetClass + " Section " + targetSection + " Roll " + targetRollNo)
                .effectiveDate(LocalDate.now())
                .actorUsername(actor != null ? actor : "system")
                .build();
        lifecycleLogRepository.save(logEntry);

        log.info("Successfully converted application {} to student {} (admissionNo={}) for tenant {}",
                applicationId, savedStudent.getId(), savedStudent.getAdmissionNo(), tenantId);

        return mapStudentToResponse(savedStudent);
    }

    private void linkPrimaryGuardian(AdmissionApplication app, Long studentId, String tenantId) {
        try {
            String contactPhone = app.getPrimaryContactPhone().trim();
            Optional<Guardian> existingGuardian = guardianRepository.findByTenantIdAndPhone(tenantId, contactPhone);

            Guardian guardian;
            if (existingGuardian.isPresent()) {
                guardian = existingGuardian.get();
            } else {
                String[] nameParts = app.getPrimaryContactName().trim().split("\\s+", 2);
                String fName = nameParts[0];
                String lName = nameParts.length > 1 ? nameParts[1] : "";

                guardian = Guardian.builder()
                        .tenantId(tenantId)
                        .firstName(fName)
                        .lastName(lName)
                        .displayName(app.getPrimaryContactName().trim())
                        .phone(contactPhone)
                        .email(app.getPrimaryContactEmail())
                        .address(app.getAddressLine1())
                        .relationshipMetadata(app.getPrimaryContactRelationship())
                        .status("ACTIVE")
                        .build();
                guardian = guardianRepository.save(guardian);
            }

            GuardianRelationship rel = parseRelationship(app.getPrimaryContactRelationship());

            StudentGuardian link = StudentGuardian.builder()
                    .studentId(studentId)
                    .guardianId(guardian.getId())
                    .tenantId(tenantId)
                    .relationshipType(rel)
                    .isPrimary(true)
                    .isEmergencyContact(true)
                    .canPickup(true)
                    .canReceiveNotifications(true)
                    .canAccessPortal(false)
                    .status("ACTIVE")
                    .build();

            studentGuardianRepository.save(link);
            log.info("Linked primary guardian {} to new student {}", guardian.getId(), studentId);
        } catch (Exception e) {
            log.warn("Non-fatal: Could not auto-link guardian during conversion: {}", e.getMessage());
        }
    }

    private GuardianRelationship parseRelationship(String relStr) {
        if (relStr == null || relStr.isBlank()) return GuardianRelationship.GUARDIAN;
        try {
            return GuardianRelationship.valueOf(relStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return GuardianRelationship.GUARDIAN;
        }
    }

    private StudentResponse mapStudentToResponse(Student s) {
        return StudentResponse.builder()
                .id(s.getId())
                .firstName(s.getFirstName())
                .lastName(s.getLastName())
                .admissionNo(s.getAdmissionNo())
                .rollNo(s.getRollNo())
                .studentClass(s.getStudentClass())
                .section(s.getSection())
                .email(s.getEmail())
                .phone(s.getPhone())
                .gender(s.getGender())
                .dob(s.getDateOfBirth())
                .address(s.getAddress())
                .status(s.getStatus())
                .build();
    }
}
