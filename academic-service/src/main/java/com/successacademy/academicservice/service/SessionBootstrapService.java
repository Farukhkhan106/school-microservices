package com.successacademy.academicservice.service;

import com.successacademy.academicservice.client.FacultyServiceClient;
import com.successacademy.academicservice.client.StudentServiceClient;
import com.successacademy.academicservice.dto.SessionBootstrapExecuteResponse;
import com.successacademy.academicservice.dto.SessionBootstrapPreviewResponse;
import com.successacademy.academicservice.model.AcademicSession;
import com.successacademy.academicservice.model.EnrollmentStatus;
import com.successacademy.academicservice.model.StudentEnrollment;
import com.successacademy.academicservice.repository.AcademicSessionRepository;
import com.successacademy.academicservice.repository.AssessmentRepository;
import com.successacademy.academicservice.repository.StudentEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionBootstrapService {

    private final AcademicSessionRepository sessionRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentServiceClient studentServiceClient;
    private final FacultyServiceClient facultyServiceClient;
    private final AssessmentRepository assessmentRepository;
    private final AuditLogService auditLogService;

    private String resolveTenant(String tenantId) {
        return (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
    }

    public SessionBootstrapPreviewResponse previewBootstrap(String tenantId) {
        String cleanTenant = resolveTenant(tenantId);

        // 1. Locate current active session
        AcademicSession activeSession = sessionRepository.findByTenantIdAndIsActiveTrue(cleanTenant)
                .or(() -> sessionRepository.findByTenantIdAndSessionCode(cleanTenant, "2026-2027"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active academic session found for tenant: " + cleanTenant));

        // 2. Fetch master students from student-service
        List<StudentServiceClient.StudentInfoDto> allStudents = studentServiceClient.getAllStudents();
        int totalMaster = allStudents != null ? allStudents.size() : 0;

        List<StudentServiceClient.StudentInfoDto> activeStudents = new ArrayList<>();
        int inactiveCount = 0;
        int invalidPlacementCount = 0;
        Set<String> distinctClasses = new HashSet<>();
        Set<String> distinctSections = new HashSet<>();

        if (allStudents != null) {
            for (var st : allStudents) {
                if ("Active".equalsIgnoreCase(st.getStatus())) {
                    if (st.getStudentClass() == null || st.getStudentClass().isBlank() ||
                        st.getSection() == null || st.getSection().isBlank()) {
                        invalidPlacementCount++;
                    } else {
                        distinctClasses.add(st.getStudentClass().trim());
                        distinctSections.add(st.getSection().trim());
                    }
                    activeStudents.add(st);
                } else {
                    inactiveCount++;
                }
            }
        }

        // 3. Check existing enrollments in active session
        List<StudentEnrollment> existingEnrollments = enrollmentRepository.findByTenantIdAndSessionId(cleanTenant, activeSession.getId());
        Set<Long> enrolledStudentIds = existingEnrollments.stream()
                .map(StudentEnrollment::getStudentId)
                .collect(Collectors.toSet());

        int alreadyEnrolled = 0;
        for (var st : activeStudents) {
            if (st.getId() != null && enrolledStudentIds.contains(st.getId())) {
                alreadyEnrolled++;
            }
        }
        int pending = activeStudents.size() - alreadyEnrolled;

        // 4. Assessments counts
        var allAssessments = assessmentRepository.findAll();
        int historicalAssessments = 0;
        int currentAssessments = 0;
        for (var a : allAssessments) {
            if (a.getSession() != null && activeSession.getId().equals(a.getSession().getId())) {
                currentAssessments++;
            } else {
                historicalAssessments++;
            }
        }

        List<String> warnings = new ArrayList<>();
        if (inactiveCount > 0) {
            warnings.add(inactiveCount + " inactive or transferred students in master directory will be safely excluded from the active session roster.");
        }
        if (invalidPlacementCount > 0) {
            warnings.add(invalidPlacementCount + " active students have incomplete class/section placement and will be logged as exceptions.");
        }

        return SessionBootstrapPreviewResponse.builder()
                .tenantId(cleanTenant)
                .activeSessionId(activeSession.getId())
                .activeSessionCode(activeSession.getSessionCode())
                .activeSessionName(activeSession.getName())
                .totalMasterStudents(totalMaster)
                .activeMasterStudents(activeStudents.size())
                .inactiveMasterStudents(inactiveCount)
                .alreadyEnrolledCount(alreadyEnrolled)
                .pendingEnrollmentCount(pending)
                .invalidPlacementCount(invalidPlacementCount)
                .ambiguousCount(0)
                .totalDistinctClasses(distinctClasses.size())
                .totalDistinctSections(distinctSections.size())
                .historicalAssessmentsCount(historicalAssessments)
                .currentSessionAssessmentsCount(currentAssessments)
                .warnings(warnings)
                .build();
    }

    @Transactional
    public SessionBootstrapExecuteResponse executeBootstrap(String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);

        // 1. Locate current active session
        AcademicSession activeSession = sessionRepository.findByTenantIdAndIsActiveTrue(cleanTenant)
                .or(() -> sessionRepository.findByTenantIdAndSessionCode(cleanTenant, "2026-2027"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active academic session found for tenant: " + cleanTenant));

        // 2. Fetch master students from student-service
        List<StudentServiceClient.StudentInfoDto> allStudents = studentServiceClient.getAllStudents();
        if (allStudents == null || allStudents.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student service returned no student profiles.");
        }

        int totalMaster = allStudents.size();
        int activeEvaluated = 0;
        int inactiveSkipped = 0;
        int preservedCount = 0;
        int newlyCreatedCount = 0;
        int ambiguousSkipped = 0;
        List<String> skippedReasons = new ArrayList<>();

        for (var st : allStudents) {
            if (st.getId() == null) continue;

            // Inactive / non-active check
            if (!"Active".equalsIgnoreCase(st.getStatus())) {
                inactiveSkipped++;
                skippedReasons.add("Student #" + st.getId() + " (" + st.getAdmissionNo() + "): Skipped because status is " + st.getStatus());
                continue;
            }

            // Placement validation
            if (st.getStudentClass() == null || st.getStudentClass().isBlank() ||
                st.getSection() == null || st.getSection().isBlank()) {
                ambiguousSkipped++;
                skippedReasons.add("Student #" + st.getId() + " (" + st.getAdmissionNo() + "): Skipped because class/section placement is incomplete");
                continue;
            }

            activeEvaluated++;

            String fullName = ((st.getFirstName() != null ? st.getFirstName() : "") + " " +
                               (st.getLastName() != null ? st.getLastName() : "")).trim();

            Optional<StudentEnrollment> existingOpt = enrollmentRepository
                    .findByTenantIdAndSessionIdAndStudentId(cleanTenant, activeSession.getId(), st.getId());

            if (existingOpt.isPresent()) {
                StudentEnrollment existing = existingOpt.get();
                existing.setStudentName(fullName);
                existing.setAdmissionNo(st.getAdmissionNo());
                existing.setStudentClass(st.getStudentClass().trim());
                existing.setSection(st.getSection().trim());
                existing.setRollNo(st.getRollNo());
                existing.setStatus(EnrollmentStatus.ACTIVE);
                enrollmentRepository.save(existing);
                preservedCount++;
            } else {
                StudentEnrollment newEnr = StudentEnrollment.builder()
                        .tenantId(cleanTenant)
                        .sessionId(activeSession.getId())
                        .sessionCode(activeSession.getSessionCode())
                        .studentId(st.getId())
                        .studentName(fullName)
                        .admissionNo(st.getAdmissionNo())
                        .studentClass(st.getStudentClass().trim())
                        .section(st.getSection().trim())
                        .rollNo(st.getRollNo())
                        .status(EnrollmentStatus.ACTIVE)
                        .enrollmentDate(LocalDate.now())
                        .build();
                enrollmentRepository.save(newEnr);
                newlyCreatedCount++;
            }
        }

        long finalCount = enrollmentRepository.countByTenantIdAndSessionId(cleanTenant, activeSession.getId());

        auditLogService.log(
                actorUserId,
                actorRole != null ? actorRole : "ADMIN",
                "FULL_ERP_SESSION_BOOTSTRAP",
                "AcademicSession",
                activeSession.getId(),
                "Partial roster (" + preservedCount + ")",
                "Full roster (" + finalCount + " students)",
                "Synchronized master students from student-service into active session " + activeSession.getSessionCode()
        );

        log.info("Session Bootstrap complete for session {}: master={}, evaluated={}, created={}, preserved={}, skipped={}, finalCount={}",
                activeSession.getSessionCode(), totalMaster, activeEvaluated, newlyCreatedCount, preservedCount, inactiveSkipped, finalCount);

        return SessionBootstrapExecuteResponse.builder()
                .tenantId(cleanTenant)
                .targetSessionId(activeSession.getId())
                .targetSessionCode(activeSession.getSessionCode())
                .targetSessionName(activeSession.getName())
                .totalMasterStudents(totalMaster)
                .activeStudentsEvaluated(activeEvaluated)
                .inactiveStudentsSkipped(inactiveSkipped)
                .alreadyEnrolledPreserved(preservedCount)
                .newlyEnrolledCreated(newlyCreatedCount)
                .finalSessionEnrollmentCount((int) finalCount)
                .ambiguousSkipped(ambiguousSkipped)
                .skippedReasons(skippedReasons)
                .executedAt(LocalDateTime.now())
                .status("COMPLETED")
                .build();
    }
}
