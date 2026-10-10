package com.successacademy.academicservice.service;

import com.successacademy.academicservice.dto.SessionRequest;
import com.successacademy.academicservice.dto.SessionResponse;
import com.successacademy.academicservice.model.AcademicSession;
import com.successacademy.academicservice.model.SessionStatus;
import com.successacademy.academicservice.repository.AcademicSessionRepository;
import com.successacademy.academicservice.repository.StudentEnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicSessionServiceTest {

    @Mock
    private AcademicSessionRepository sessionRepository;

    @Mock
    private StudentEnrollmentRepository enrollmentRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AcademicSessionService sessionService;

    private AcademicSession active2026;
    private AcademicSession upcoming2027;

    @BeforeEach
    void setUp() {
        active2026 = AcademicSession.builder()
                .id(1L)
                .tenantId("tenant_a")
                .sessionCode("2026-2027")
                .name("Academic Session 2026-27")
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2027, 3, 31))
                .status(SessionStatus.ACTIVE)
                .isActive(true)
                .build();

        upcoming2027 = AcademicSession.builder()
                .id(2L)
                .tenantId("tenant_a")
                .sessionCode("2027-2028")
                .name("Academic Session 2027-28")
                .startDate(LocalDate.of(2027, 4, 1))
                .endDate(LocalDate.of(2028, 3, 31))
                .status(SessionStatus.UPCOMING)
                .isActive(false)
                .build();
    }

    @Test
    @DisplayName("Create Session - Successfully creates session when dates are valid")
    void testCreateSession_Success() {
        SessionRequest req = SessionRequest.builder()
                .sessionCode("2028-2029")
                .name("Academic Session 2028-29")
                .startDate(LocalDate.of(2028, 4, 1))
                .endDate(LocalDate.of(2029, 3, 31))
                .status(SessionStatus.UPCOMING)
                .isActive(false)
                .build();

        when(sessionRepository.existsByTenantIdAndSessionCode("tenant_a", "2028-2029")).thenReturn(false);
        when(sessionRepository.save(any(AcademicSession.class))).thenAnswer(invocation -> {
            AcademicSession s = invocation.getArgument(0);
            s.setId(3L);
            return s;
        });

        SessionResponse resp = sessionService.createSession(req, "tenant_a", 100L, "ADMIN");

        assertThat(resp).isNotNull();
        assertThat(resp.getSessionCode()).isEqualTo("2028-2029");
        assertThat(resp.getStatus()).isEqualTo(SessionStatus.UPCOMING);
        assertThat(resp.isActive()).isFalse();
        verify(sessionRepository).save(any(AcademicSession.class));
    }

    @Test
    @DisplayName("Create Session - Fails when startDate is equal to or after endDate")
    void testCreateSession_InvalidDateRange_ThrowsBadRequest() {
        SessionRequest req = SessionRequest.builder()
                .sessionCode("2028-2029")
                .name("Academic Session 2028-29")
                .startDate(LocalDate.of(2029, 4, 1))
                .endDate(LocalDate.of(2028, 3, 31)) // invalid!
                .build();

        assertThatThrownBy(() -> sessionService.createSession(req, "tenant_a", 100L, "ADMIN"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("strictly before end date");
    }

    @Test
    @DisplayName("Create Session - Fails when duplicate sessionCode exists in same tenant")
    void testCreateSession_DuplicateCode_ThrowsConflict() {
        SessionRequest req = SessionRequest.builder()
                .sessionCode("2026-2027")
                .name("Academic Session 2026-27 Duplicate")
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2027, 3, 31))
                .build();

        when(sessionRepository.existsByTenantIdAndSessionCode("tenant_a", "2026-2027")).thenReturn(true);

        assertThatThrownBy(() -> sessionService.createSession(req, "tenant_a", 100L, "ADMIN"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists for this school");
    }

    @Test
    @DisplayName("Activate Session - Closes previous active session and activates target")
    void testActivateSession_EnforcesSingleActiveRule() {
        when(sessionRepository.findById(2L)).thenReturn(Optional.of(upcoming2027));
        when(sessionRepository.findByTenantIdAndIsActiveTrue("tenant_a")).thenReturn(Optional.of(active2026));
        when(sessionRepository.save(any(AcademicSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse resp = sessionService.activateSession(2L, "tenant_a", 100L, "ADMIN");

        assertThat(resp.isActive()).isTrue();
        assertThat(resp.getStatus()).isEqualTo(SessionStatus.ACTIVE);

        // Previous active session must have been closed
        assertThat(active2026.isActive()).isFalse();
        assertThat(active2026.getStatus()).isEqualTo(SessionStatus.CLOSED);

        verify(sessionRepository, times(2)).save(any(AcademicSession.class));
    }

    @Test
    @DisplayName("Activate Session - Rejects activating ARCHIVED session")
    void testActivateSession_Archived_ThrowsBadRequest() {
        AcademicSession archived = AcademicSession.builder()
                .id(9L)
                .tenantId("tenant_a")
                .sessionCode("2020-2021")
                .status(SessionStatus.ARCHIVED)
                .build();

        when(sessionRepository.findById(9L)).thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> sessionService.activateSession(9L, "tenant_a", 100L, "ADMIN"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot activate an ARCHIVED session");
    }

    @Test
    @DisplayName("Update Session - Rejects modifying ARCHIVED session")
    void testUpdateSession_Archived_Forbidden() {
        AcademicSession archived = AcademicSession.builder()
                .id(9L)
                .tenantId("tenant_a")
                .sessionCode("2020-2021")
                .status(SessionStatus.ARCHIVED)
                .build();

        when(sessionRepository.findById(9L)).thenReturn(Optional.of(archived));

        SessionRequest req = SessionRequest.builder()
                .name("Try modifying")
                .build();

        assertThatThrownBy(() -> sessionService.updateSession(9L, req, "tenant_a", 100L, "ADMIN"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Archived academic sessions are immutable");
    }

    @Test
    @DisplayName("Tenant Isolation - Cannot access session belonging to another tenant")
    void testTenantIsolation_CannotAccessOtherTenant() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(active2026));

        // Attempting access from tenant_b when session belongs to tenant_a
        assertThatThrownBy(() -> sessionService.getSessionById(1L, "tenant_b"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }
}
