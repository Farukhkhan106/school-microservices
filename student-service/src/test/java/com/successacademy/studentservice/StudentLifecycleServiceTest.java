package com.successacademy.studentservice;

import com.successacademy.studentservice.dto.StudentLifecycleActionRequest;
import com.successacademy.studentservice.model.Student;
import com.successacademy.studentservice.model.StudentLifecycleLog;
import com.successacademy.studentservice.repository.StudentLifecycleLogRepository;
import com.successacademy.studentservice.repository.StudentRepository;
import com.successacademy.studentservice.security.TenantContext;
import com.successacademy.studentservice.service.StudentLifecycleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentLifecycleServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentLifecycleLogRepository lifecycleLogRepository;

    @InjectMocks
    private StudentLifecycleService lifecycleService;

    @BeforeEach
    void setup() {
        TenantContext.setTenantId("tenant-test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Transfer student: ACTIVE -> TRANSFERRED with destination institution & reason")
    void testTransferStudent() {
        Student student = Student.builder()
                .id(601L)
                .tenantId("tenant-test")
                .status("ACTIVE")
                .lifecycleStatus("ACTIVE")
                .build();

        when(studentRepository.findByIdAndTenantId(601L, "tenant-test")).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentLifecycleActionRequest req = StudentLifecycleActionRequest.builder()
                .action("TRANSFER")
                .effectiveDate(LocalDate.now())
                .reason("Relocating to another state")
                .destinationInstitution("St. Xavier's International School")
                .notes("Transfer certificate issued")
                .build();

        Student updated = lifecycleService.executeLifecycleAction(601L, req, "admin_user");

        assertNotNull(updated);
        assertEquals("TRANSFERRED", updated.getLifecycleStatus());
        assertEquals("INACTIVE", updated.getStatus()); // backward compatibility

        verify(lifecycleLogRepository).save(argThat(log ->
                "TRANSFER".equals(log.getAction()) &&
                "ACTIVE".equals(log.getFromStatus()) &&
                "TRANSFERRED".equals(log.getToStatus()) &&
                "St. Xavier's International School".equals(log.getDestinationInstitution()) &&
                "admin_user".equals(log.getActorUsername())));
    }

    @Test
    @DisplayName("Withdraw student: ACTIVE -> WITHDRAWN")
    void testWithdrawStudent() {
        Student student = Student.builder()
                .id(602L)
                .tenantId("tenant-test")
                .status("ACTIVE")
                .lifecycleStatus("ACTIVE")
                .build();

        when(studentRepository.findByIdAndTenantId(602L, "tenant-test")).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentLifecycleActionRequest req = StudentLifecycleActionRequest.builder()
                .action("WITHDRAW")
                .effectiveDate(LocalDate.now())
                .reason("Personal family reasons")
                .build();

        Student updated = lifecycleService.executeLifecycleAction(602L, req, "admin_user");

        assertEquals("WITHDRAWN", updated.getLifecycleStatus());
        assertEquals("INACTIVE", updated.getStatus());
    }

    @Test
    @DisplayName("Graduate student: ACTIVE -> GRADUATED")
    void testGraduateStudent() {
        Student student = Student.builder()
                .id(603L)
                .tenantId("tenant-test")
                .status("ACTIVE")
                .lifecycleStatus("ACTIVE")
                .build();

        when(studentRepository.findByIdAndTenantId(603L, "tenant-test")).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        StudentLifecycleActionRequest req = StudentLifecycleActionRequest.builder()
                .action("GRADUATE")
                .effectiveDate(LocalDate.now())
                .reason("Completed Class 12 Senior Secondary Curriculum")
                .build();

        Student updated = lifecycleService.executeLifecycleAction(603L, req, "admin_user");

        assertEquals("GRADUATED", updated.getLifecycleStatus());
    }

    @Test
    @DisplayName("Terminal protection: GRADUATED student cannot be transferred or withdrawn")
    void testGraduatedStudentCannotBeTransferred() {
        Student student = Student.builder()
                .id(604L)
                .tenantId("tenant-test")
                .status("INACTIVE")
                .lifecycleStatus("GRADUATED")
                .build();

        when(studentRepository.findByIdAndTenantId(604L, "tenant-test")).thenReturn(Optional.of(student));

        StudentLifecycleActionRequest req = StudentLifecycleActionRequest.builder()
                .action("TRANSFER")
                .reason("Invalid attempt")
                .build();

        assertThrows(ResponseStatusException.class, () ->
                lifecycleService.executeLifecycleAction(604L, req, "admin"));
    }
}
