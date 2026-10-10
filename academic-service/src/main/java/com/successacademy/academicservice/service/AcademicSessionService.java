package com.successacademy.academicservice.service;

import com.successacademy.academicservice.dto.SessionRequest;
import com.successacademy.academicservice.dto.SessionResponse;
import com.successacademy.academicservice.dto.StudentEnrollmentRequest;
import com.successacademy.academicservice.dto.StudentEnrollmentResponse;
import com.successacademy.academicservice.model.AcademicSession;
import com.successacademy.academicservice.model.EnrollmentStatus;
import com.successacademy.academicservice.model.SessionStatus;
import com.successacademy.academicservice.model.StudentEnrollment;
import com.successacademy.academicservice.repository.AcademicSessionRepository;
import com.successacademy.academicservice.repository.StudentEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicSessionService {

    private final AcademicSessionRepository sessionRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AuditLogService auditLogService;

    // Backward-compatible calls defaulting to "default" tenant
    public List<SessionResponse> getAllSessions() {
        return getAllSessions("default");
    }

    public List<SessionResponse> getAllSessions(String tenantId) {
        String cleanTenant = resolveTenant(tenantId);
        return sessionRepository.findByTenantIdOrderByStartDateDesc(cleanTenant).stream()
                .map(s -> mapToResponse(s, cleanTenant))
                .toList();
    }

    public SessionResponse getActiveSession() {
        return getActiveSession("default");
    }

    public SessionResponse getActiveSession(String tenantId) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSession session = sessionRepository.findByTenantIdAndIsActiveTrue(cleanTenant)
                .or(() -> sessionRepository.findByTenantIdAndStatus(cleanTenant, SessionStatus.ACTIVE))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active academic session found for tenant: " + cleanTenant));
        return mapToResponse(session, cleanTenant);
    }

    public SessionResponse getSessionById(Long id) {
        return getSessionById(id, "default");
    }

    public SessionResponse getSessionById(Long id, String tenantId) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSession session = sessionRepository.findById(id)
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic session not found with ID: " + id));
        return mapToResponse(session, cleanTenant);
    }

    @Transactional
    public SessionResponse createSession(SessionRequest request, Long actorUserId, String actorRole) {
        return createSession(request, "default", actorUserId, actorRole);
    }

    @Transactional
    public SessionResponse createSession(SessionRequest request, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId != null ? tenantId : request.getTenantId());
        String code = request.getSessionCode() != null ? request.getSessionCode().trim() : "";

        if (code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session code is required.");
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both start date and end date are required.");
        }

        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session start date must be strictly before end date.");
        }

        if (sessionRepository.existsByTenantIdAndSessionCode(cleanTenant, code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Academic session with code '" + code + "' already exists for this school.");
        }

        SessionStatus initialStatus = request.getStatus() != null
                ? request.getStatus()
                : (request.isActive() ? SessionStatus.ACTIVE : SessionStatus.UPCOMING);

        // If newly created session is set to ACTIVE, deactivate currently active session for this tenant
        if (initialStatus == SessionStatus.ACTIVE || request.isActive()) {
            sessionRepository.findByTenantIdAndIsActiveTrue(cleanTenant).ifPresent(prev -> {
                prev.setActive(false);
                prev.setStatus(SessionStatus.CLOSED);
                sessionRepository.save(prev);
                log.info("Closed previous active session {} upon new active session creation for tenant {}", prev.getSessionCode(), cleanTenant);
            });
            initialStatus = SessionStatus.ACTIVE;
        }

        AcademicSession session = AcademicSession.builder()
                .tenantId(cleanTenant)
                .sessionCode(code)
                .name(request.getName().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(initialStatus)
                .isActive(initialStatus == SessionStatus.ACTIVE)
                .description(request.getDescription())
                .createdBy(actorUserId)
                .build();

        AcademicSession saved = sessionRepository.save(session);
        auditLogService.log(actorUserId, actorRole, "SESSION_CREATED", "AcademicSession", saved.getId(), null, saved.getSessionCode(), "Created session in " + cleanTenant);

        return mapToResponse(saved, cleanTenant);
    }

    @Transactional
    public SessionResponse updateSession(Long id, SessionRequest request, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSession session = sessionRepository.findById(id)
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic session not found with ID: " + id));

        if (session.getStatus() == SessionStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Archived academic sessions are immutable and read-only.");
        }

        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (!request.getStartDate().isBefore(request.getEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session start date must be strictly before end date.");
            }
            session.setStartDate(request.getStartDate());
            session.setEndDate(request.getEndDate());
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            session.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            session.setDescription(request.getDescription().trim());
        }

        session.setUpdatedBy(actorUserId);
        AcademicSession saved = sessionRepository.save(session);
        auditLogService.log(actorUserId, actorRole, "SESSION_UPDATED", "AcademicSession", saved.getId(), null, saved.getSessionCode(), "Updated session metadata");

        return mapToResponse(saved, cleanTenant);
    }

    @Transactional
    public SessionResponse activateSession(Long id, Long actorUserId, String actorRole) {
        return activateSession(id, "default", actorUserId, actorRole);
    }

    @Transactional
    public SessionResponse activateSession(Long id, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSession session = sessionRepository.findById(id)
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic session not found with ID: " + id));

        if (session.getStatus() == SessionStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot activate an ARCHIVED session directly.");
        }

        // Close any currently active session in this tenant
        sessionRepository.findByTenantIdAndIsActiveTrue(cleanTenant).ifPresent(currentActive -> {
            if (!currentActive.getId().equals(id)) {
                currentActive.setActive(false);
                currentActive.setStatus(SessionStatus.CLOSED);
                sessionRepository.save(currentActive);
                auditLogService.log(actorUserId, actorRole, "SESSION_CLOSED", "AcademicSession", currentActive.getId(), null, currentActive.getSessionCode(), "Closed session due to new activation");
            }
        });

        session.setActive(true);
        session.setStatus(SessionStatus.ACTIVE);
        session.setUpdatedBy(actorUserId);

        AcademicSession saved = sessionRepository.save(session);
        auditLogService.log(actorUserId, actorRole, "SESSION_ACTIVATED", "AcademicSession", saved.getId(), null, saved.getSessionCode(), "Activated session in " + cleanTenant);

        return mapToResponse(saved, cleanTenant);
    }

    @Transactional
    public SessionResponse closeSession(Long id, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSession session = sessionRepository.findById(id)
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic session not found with ID: " + id));

        session.setActive(false);
        session.setStatus(SessionStatus.CLOSED);
        session.setUpdatedBy(actorUserId);

        AcademicSession saved = sessionRepository.save(session);
        auditLogService.log(actorUserId, actorRole, "SESSION_CLOSED", "AcademicSession", saved.getId(), null, saved.getSessionCode(), "Closed session");

        return mapToResponse(saved, cleanTenant);
    }

    @Transactional
    public SessionResponse archiveSession(Long id, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSession session = sessionRepository.findById(id)
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic session not found with ID: " + id));

        session.setActive(false);
        session.setStatus(SessionStatus.ARCHIVED);
        session.setUpdatedBy(actorUserId);

        AcademicSession saved = sessionRepository.save(session);
        auditLogService.log(actorUserId, actorRole, "SESSION_ARCHIVED", "AcademicSession", saved.getId(), null, saved.getSessionCode(), "Archived session");

        return mapToResponse(saved, cleanTenant);
    }

    // ── ENROLLMENTS ──────────────────────────────────────────────

    public List<StudentEnrollmentResponse> getEnrollmentsBySession(Long sessionId, String studentClass, String section, String tenantId) {
        String cleanTenant = resolveTenant(tenantId);
        List<StudentEnrollment> list;
        if (studentClass != null && !studentClass.isBlank() && section != null && !section.isBlank()) {
            list = enrollmentRepository.findByTenantIdAndSessionIdAndStudentClassAndSection(cleanTenant, sessionId, studentClass.trim(), section.trim());
        } else if (studentClass != null && !studentClass.isBlank()) {
            list = enrollmentRepository.findByTenantIdAndSessionIdAndStudentClass(cleanTenant, sessionId, studentClass.trim());
        } else {
            list = enrollmentRepository.findByTenantIdAndSessionId(cleanTenant, sessionId);
        }
        return list.stream().map(this::mapEnrollmentToResponse).toList();
    }

    public List<StudentEnrollmentResponse> getStudentEnrollmentHistory(Long studentId, String tenantId) {
        String cleanTenant = resolveTenant(tenantId);
        return enrollmentRepository.findByTenantIdAndStudentIdOrderByCreatedAtDesc(cleanTenant, studentId).stream()
                .map(this::mapEnrollmentToResponse)
                .toList();
    }

    @Transactional
    public StudentEnrollmentResponse enrollStudent(StudentEnrollmentRequest req, String tenantId) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSession session = sessionRepository.findById(req.getSessionId())
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getStatus() == SessionStatus.ARCHIVED || session.getStatus() == SessionStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot enroll student in a closed or archived session.");
        }

        StudentEnrollment enrollment = enrollmentRepository.findByTenantIdAndSessionIdAndStudentId(cleanTenant, req.getSessionId(), req.getStudentId())
                .orElseGet(() -> StudentEnrollment.builder()
                        .tenantId(cleanTenant)
                        .sessionId(session.getId())
                        .sessionCode(session.getSessionCode())
                        .studentId(req.getStudentId())
                        .build());

        enrollment.setStudentName(req.getStudentName());
        enrollment.setAdmissionNo(req.getAdmissionNo());
        enrollment.setStudentClass(req.getStudentClass().trim());
        enrollment.setSection(req.getSection().trim());
        enrollment.setRollNo(req.getRollNo());
        enrollment.setStatus(req.getStatus() != null ? req.getStatus() : EnrollmentStatus.ACTIVE);
        enrollment.setRemarks(req.getRemarks());

        StudentEnrollment saved = enrollmentRepository.save(enrollment);
        return mapEnrollmentToResponse(saved);
    }

    // ── HELPERS ──────────────────────────────────────────────────

    private String resolveTenant(String tenantId) {
        return (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
    }

    private SessionResponse mapToResponse(AcademicSession s, String tenantId) {
        long enrollmentCount = enrollmentRepository.countByTenantIdAndSessionId(tenantId, s.getId());
        return SessionResponse.builder()
                .id(s.getId())
                .tenantId(s.getTenantId())
                .sessionCode(s.getSessionCode())
                .name(s.getName())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .status(s.getStatus())
                .isActive(s.isActive())
                .description(s.getDescription())
                .studentEnrollmentCount(enrollmentCount)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private StudentEnrollmentResponse mapEnrollmentToResponse(StudentEnrollment e) {
        return StudentEnrollmentResponse.builder()
                .id(e.getId())
                .tenantId(e.getTenantId())
                .sessionId(e.getSessionId())
                .sessionCode(e.getSessionCode())
                .studentId(e.getStudentId())
                .studentName(e.getStudentName())
                .admissionNo(e.getAdmissionNo())
                .studentClass(e.getStudentClass())
                .section(e.getSection())
                .rollNo(e.getRollNo())
                .status(e.getStatus())
                .enrollmentDate(e.getEnrollmentDate())
                .withdrawalDate(e.getWithdrawalDate())
                .withdrawalReason(e.getWithdrawalReason())
                .remarks(e.getRemarks())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
