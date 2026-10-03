package com.successacademy.academicservice.controller;

import com.successacademy.academicservice.dto.SessionRequest;
import com.successacademy.academicservice.dto.SessionResponse;
import com.successacademy.academicservice.security.SecurityContextUtil;
import com.successacademy.academicservice.service.AcademicSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/academic/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final AcademicSessionService sessionService;
    private final SecurityContextUtil securityUtil;

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getAllSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    @GetMapping("/active")
    public ResponseEntity<SessionResponse> getActiveSession() {
        return ResponseEntity.ok(sessionService.getActiveSession());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSessionById(id));
    }

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
            @Valid @RequestBody SessionRequest request,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.createSession(request, actorUserId, role));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<SessionResponse> activateSession(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        String role = securityUtil.getCurrentRole(req);
        return ResponseEntity.ok(sessionService.activateSession(id, actorUserId, role));
    }
}
