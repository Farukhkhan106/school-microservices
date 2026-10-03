package com.successacademy.academicservice.service;

import com.successacademy.academicservice.dto.SessionRequest;
import com.successacademy.academicservice.dto.SessionResponse;
import com.successacademy.academicservice.model.AcademicSession;
import com.successacademy.academicservice.repository.AcademicSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicSessionService {

    private final AcademicSessionRepository sessionRepository;
    private final AuditLogService auditLogService;

    public List<SessionResponse> getAllSessions() {
        return sessionRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public SessionResponse getActiveSession() {
        AcademicSession session = sessionRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active academic session found."));
        return mapToResponse(session);
    }

    public SessionResponse getSessionById(Long id) {
        AcademicSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic session not found with ID: " + id));
        return mapToResponse(session);
    }

    @Transactional
    public SessionResponse createSession(SessionRequest request, Long actorUserId, String actorRole) {
        if (sessionRepository.findBySessionCode(request.getSessionCode().trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Academic session with code '" + request.getSessionCode() + "' already exists.");
        }

        if (request.isActive()) {
            // Deactivate existing active sessions
            sessionRepository.findByIsActiveTrue().ifPresent(s -> {
                s.setActive(false);
                sessionRepository.save(s);
            });
        }

        AcademicSession session = AcademicSession.builder()
                .sessionCode(request.getSessionCode().trim())
                .name(request.getName().trim())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(request.isActive())
                .description(request.getDescription())
                .build();

        AcademicSession saved = sessionRepository.save(session);
        auditLogService.log(actorUserId, actorRole, "SESSION_CREATED", "AcademicSession", saved.getId(), null, saved.getSessionCode(), "Created session");

        return mapToResponse(saved);
    }

    @Transactional
    public SessionResponse activateSession(Long id, Long actorUserId, String actorRole) {
        AcademicSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic session not found with ID: " + id));

        sessionRepository.findByIsActiveTrue().ifPresent(s -> {
            if (!s.getId().equals(id)) {
                s.setActive(false);
                sessionRepository.save(s);
            }
        });

        session.setActive(true);
        AcademicSession saved = sessionRepository.save(session);
        auditLogService.log(actorUserId, actorRole, "SESSION_ACTIVATED", "AcademicSession", saved.getId(), null, saved.getSessionCode(), "Activated session");

        return mapToResponse(saved);
    }

    private SessionResponse mapToResponse(AcademicSession s) {
        return SessionResponse.builder()
                .id(s.getId())
                .sessionCode(s.getSessionCode())
                .name(s.getName())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .isActive(s.isActive())
                .description(s.getDescription())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
