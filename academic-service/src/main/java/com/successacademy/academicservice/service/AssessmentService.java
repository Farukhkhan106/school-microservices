package com.successacademy.academicservice.service;

import com.successacademy.academicservice.dto.AssessmentRequest;
import com.successacademy.academicservice.dto.AssessmentResponse;
import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.AcademicSessionRepository;
import com.successacademy.academicservice.repository.AssessmentRepository;
import com.successacademy.academicservice.repository.GradingSchemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AcademicSessionRepository sessionRepository;
    private final GradingSchemeRepository gradingSchemeRepository;
    private final AuditLogService auditLogService;

    public List<AssessmentResponse> getAssessmentsBySession(Long sessionId) {
        return assessmentRepository.findBySessionIdOrderByStartDateDesc(sessionId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public AssessmentResponse getAssessmentById(Long id) {
        Assessment a = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found with ID: " + id));
        return mapToResponse(a);
    }

    @Transactional
    public AssessmentResponse createAssessment(AssessmentRequest req, Long actorUserId, String actorRole) {
        AcademicSession session = sessionRepository.findById(req.getSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic session not found with ID: " + req.getSessionId()));

        GradingScheme scheme = null;
        if (req.getGradingSchemeId() != null) {
            scheme = gradingSchemeRepository.findById(req.getGradingSchemeId()).orElse(null);
        }
        if (scheme == null) {
            scheme = gradingSchemeRepository.findByIsDefaultTrue().orElse(null);
        }

        Assessment assessment = Assessment.builder()
                .session(session)
                .name(req.getName().trim())
                .assessmentType(req.getAssessmentType())
                .term(req.getTerm())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .gradingScheme(scheme)
                .isRankVisible(req.getIsRankVisible() != null ? req.getIsRankVisible() : true)
                .description(req.getDescription())
                .status(AssessmentStatus.SCHEDULED)
                .createdBy(actorUserId)
                .build();

        Assessment saved = assessmentRepository.save(assessment);
        auditLogService.log(actorUserId, actorRole, "ASSESSMENT_CREATED", "Assessment", saved.getId(), null, saved.getName(), "Created assessment");

        return mapToResponse(saved);
    }

    @Transactional
    public AssessmentResponse updateStatus(Long id, AssessmentStatus status, Long actorUserId, String actorRole) {
        Assessment a = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found with ID: " + id));

        AssessmentStatus oldStatus = a.getStatus();
        a.setStatus(status);
        Assessment saved = assessmentRepository.save(a);

        auditLogService.log(actorUserId, actorRole, "ASSESSMENT_STATUS_CHANGED", "Assessment", saved.getId(), oldStatus.name(), status.name(), "Changed status");
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteAssessment(Long id, Long actorUserId, String actorRole) {
        Assessment a = assessmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found with ID: " + id));

        boolean hasMarks = a.getSchedules().stream().anyMatch(s -> !s.getMarks().isEmpty());
        if (hasMarks) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete assessment that already contains student marks. Archive it instead.");
        }

        assessmentRepository.delete(a);
        auditLogService.log(actorUserId, actorRole, "ASSESSMENT_DELETED", "Assessment", id, a.getName(), null, "Deleted assessment");
    }

    public AssessmentResponse mapToResponse(Assessment a) {
        int totalSchedules = a.getSchedules() != null ? a.getSchedules().size() : 0;
        int submittedSchedules = a.getSchedules() != null
                ? (int) a.getSchedules().stream().filter(s -> s.getStatus() == MarkStatus.SUBMITTED || s.getStatus() == MarkStatus.VERIFIED || s.getStatus() == MarkStatus.LOCKED).count()
                : 0;

        return AssessmentResponse.builder()
                .id(a.getId())
                .sessionId(a.getSession().getId())
                .sessionCode(a.getSession().getSessionCode())
                .name(a.getName())
                .assessmentType(a.getAssessmentType())
                .assessmentTypeDisplayName(a.getAssessmentType().getDisplayName())
                .term(a.getTerm())
                .startDate(a.getStartDate())
                .endDate(a.getEndDate())
                .gradingSchemeId(a.getGradingScheme() != null ? a.getGradingScheme().getId() : null)
                .gradingSchemeName(a.getGradingScheme() != null ? a.getGradingScheme().getName() : "Standard CBSE")
                .isRankVisible(a.isRankVisible())
                .status(a.getStatus())
                .description(a.getDescription())
                .totalSchedulesCount(totalSchedules)
                .submittedSchedulesCount(submittedSchedules)
                .createdAt(a.getCreatedAt())
                .build();
    }
}
