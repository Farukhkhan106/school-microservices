package com.successacademy.studentservice;

import com.successacademy.studentservice.dto.GuardianRequest;
import com.successacademy.studentservice.dto.GuardianResponse;
import com.successacademy.studentservice.dto.StudentGuardianDto;
import com.successacademy.studentservice.dto.StudentGuardianLinkRequest;
import com.successacademy.studentservice.model.Guardian;
import com.successacademy.studentservice.model.GuardianRelationship;
import com.successacademy.studentservice.model.Student;
import com.successacademy.studentservice.model.StudentGuardian;
import com.successacademy.studentservice.repository.GuardianRepository;
import com.successacademy.studentservice.repository.StudentGuardianRepository;
import com.successacademy.studentservice.repository.StudentRepository;
import com.successacademy.studentservice.security.TenantContext;
import com.successacademy.studentservice.service.GuardianService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuardianServiceTest {

    @Mock
    private GuardianRepository guardianRepository;

    @Mock
    private StudentGuardianRepository studentGuardianRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private GuardianService guardianService;

    @BeforeEach
    void setup() {
        TenantContext.setTenantId("tenant-test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Create Guardian with tenant isolation")
    void testCreateGuardian() {
        GuardianRequest req = GuardianRequest.builder()
                .firstName("Sarah")
                .lastName("Connor")
                .phone("9988776655")
                .email("sarah@resistance.org")
                .address("Los Angeles")
                .build();

        when(guardianRepository.save(any(Guardian.class))).thenAnswer(inv -> {
            Guardian g = inv.getArgument(0);
            g.setId(501L);
            return g;
        });

        GuardianResponse resp = guardianService.createGuardian(req);

        assertNotNull(resp);
        assertEquals(501L, resp.getId());
        assertEquals("Sarah Connor", resp.getDisplayName());
        assertEquals("9988776655", resp.getPhone());
        assertEquals("tenant-test", resp.getTenantId());
    }

    @Test
    @DisplayName("Link Guardian to Student with relationship metadata")
    void testLinkGuardianToStudent() {
        Student student = Student.builder().id(111L).tenantId("tenant-test").build();
        Guardian guardian = Guardian.builder().id(501L).tenantId("tenant-test").firstName("Sarah").lastName("Connor").build();

        when(studentRepository.findByIdAndTenantId(111L, "tenant-test")).thenReturn(Optional.of(student));
        when(guardianRepository.findByIdAndTenantId(501L, "tenant-test")).thenReturn(Optional.of(guardian));
        when(studentGuardianRepository.findByStudentIdAndGuardianIdAndTenantId(111L, 501L, "tenant-test"))
                .thenReturn(Optional.empty());
        when(studentGuardianRepository.save(any(StudentGuardian.class))).thenAnswer(inv -> {
            StudentGuardian sg = inv.getArgument(0);
            sg.setId(999L);
            return sg;
        });

        StudentGuardianLinkRequest linkReq = StudentGuardianLinkRequest.builder()
                .guardianId(501L)
                .relationshipType(GuardianRelationship.MOTHER)
                .isPrimary(true)
                .isEmergencyContact(true)
                .canPickup(true)
                .build();

        StudentGuardianDto dto = guardianService.linkGuardianToStudent(111L, linkReq);

        assertNotNull(dto);
        assertEquals(111L, dto.getStudentId());
        assertEquals(501L, dto.getGuardianId());
        assertEquals(GuardianRelationship.MOTHER, dto.getRelationshipType());
        assertTrue(dto.getIsPrimary());
        assertTrue(dto.getIsEmergencyContact());
    }

    @Test
    @DisplayName("Cross-tenant link rejected: Student does not belong to tenant")
    void testCrossTenantStudentLinkRejected() {
        when(studentRepository.findByIdAndTenantId(112L, "tenant-test")).thenReturn(Optional.empty());

        StudentGuardianLinkRequest linkReq = StudentGuardianLinkRequest.builder()
                .guardianId(501L)
                .relationshipType(GuardianRelationship.FATHER)
                .build();

        assertThrows(ResponseStatusException.class, () ->
                guardianService.linkGuardianToStudent(112L, linkReq));
    }

    @Test
    @DisplayName("Ownership check: verify guardian is linked to student")
    void testGuardianStudentOwnershipCheck() {
        when(studentGuardianRepository.findByStudentIdAndGuardianIdAndTenantId(111L, 501L, "tenant-test"))
                .thenReturn(Optional.of(StudentGuardian.builder().id(1L).build()));

        assertTrue(guardianService.isGuardianLinkedToStudent(501L, 111L, "tenant-test"));
        assertFalse(guardianService.isGuardianLinkedToStudent(502L, 111L, "tenant-test"));
    }
}
