package com.successacademy.studentservice;

import com.successacademy.studentservice.client.AcademicServiceClient;
import com.successacademy.studentservice.client.AuthServiceClient;
import com.successacademy.studentservice.dto.AdmissionConversionRequest;
import com.successacademy.studentservice.dto.StudentResponse;
import com.successacademy.studentservice.model.AdmissionApplication;
import com.successacademy.studentservice.model.AdmissionStatus;
import com.successacademy.studentservice.model.Guardian;
import com.successacademy.studentservice.model.Student;
import com.successacademy.studentservice.repository.*;
import com.successacademy.studentservice.security.TenantContext;
import com.successacademy.studentservice.service.AdmissionConversionService;
import com.successacademy.studentservice.service.AdmissionSequenceService;
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
class AdmissionConversionServiceTest {

    @Mock
    private AdmissionApplicationRepository applicationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private GuardianRepository guardianRepository;

    @Mock
    private StudentGuardianRepository studentGuardianRepository;

    @Mock
    private StudentLifecycleLogRepository lifecycleLogRepository;

    @Mock
    private AdmissionSequenceService sequenceService;

    @Mock
    private AcademicServiceClient academicServiceClient;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private AdmissionConversionService conversionService;

    @BeforeEach
    void setup() {
        TenantContext.setTenantId("tenant-test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Conversion must fail if application is not APPROVED")
    void testConversionRequiresApprovedStatus() {
        AdmissionApplication app = AdmissionApplication.builder()
                .id(301L)
                .tenantId("tenant-test")
                .status(AdmissionStatus.UNDER_REVIEW)
                .build();

        when(applicationRepository.findByIdAndTenantId(301L, "tenant-test")).thenReturn(Optional.of(app));

        assertThrows(ResponseStatusException.class, () ->
                conversionService.convertToStudent(301L, new AdmissionConversionRequest(), "admin"));
    }

    @Test
    @DisplayName("Conversion is Idempotent: Subsequent conversion call returns existing student")
    void testIdempotentConversion() {
        AdmissionApplication app = AdmissionApplication.builder()
                .id(302L)
                .tenantId("tenant-test")
                .status(AdmissionStatus.CONVERTED)
                .convertedStudentId(555L)
                .build();

        Student existingStudent = Student.builder()
                .id(555L)
                .tenantId("tenant-test")
                .firstName("Leo")
                .lastName("Messi")
                .admissionNo("SA-2026-0010")
                .studentClass("10")
                .section("A")
                .rollNo("10")
                .status("ACTIVE")
                .build();

        when(applicationRepository.findByIdAndTenantId(302L, "tenant-test")).thenReturn(Optional.of(app));
        when(studentRepository.findByIdAndTenantId(555L, "tenant-test")).thenReturn(Optional.of(existingStudent));

        StudentResponse resp = conversionService.convertToStudent(302L, new AdmissionConversionRequest(), "admin");

        assertNotNull(resp);
        assertEquals(555L, resp.getId());
        assertEquals("SA-2026-0010", resp.getAdmissionNo());
        // Verify no new student was saved
        verify(studentRepository, never()).save(any());
        verify(sequenceService, never()).nextAdmissionNumber(anyString());
    }

    @Test
    @DisplayName("Transactional conversion creates Student, Guardian Link, Enrollment and Log")
    void testCompleteConversionWorkflow() {
        AdmissionApplication app = AdmissionApplication.builder()
                .id(303L)
                .tenantId("tenant-test")
                .applicationNumber("ADM-2026-000303")
                .academicSessionId(1L)
                .applyingClass("9")
                .status(AdmissionStatus.APPROVED)
                .firstName("Kylian")
                .lastName("Mbappe")
                .dateOfBirth(LocalDate.of(2011, 12, 20))
                .gender("MALE")
                .fatherName("Wilfried Mbappe")
                .fatherPhone("9123456789")
                .fatherEmail("wilfried@example.com")
                .addressLine1("Bondy Paris")
                .city("Paris")
                .state("IDF")
                .pincode("75001")
                .build();

        when(applicationRepository.findByIdAndTenantId(303L, "tenant-test")).thenReturn(Optional.of(app));
        when(sequenceService.nextAdmissionNumber("tenant-test")).thenReturn("SA-2026-0007");
        when(studentRepository.countByTenantIdAndStudentClassAndSection("tenant-test", "9", "A")).thenReturn(6L);

        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(777L);
            return s;
        });

        when(guardianRepository.findByTenantIdAndPhone(eq("tenant-test"), eq("9123456789"))).thenReturn(Optional.empty());
        when(guardianRepository.save(any(Guardian.class))).thenAnswer(inv -> {
            Guardian g = inv.getArgument(0);
            g.setId(888L);
            return g;
        });

        AdmissionConversionRequest request = AdmissionConversionRequest.builder()
                .studentClass("9")
                .section("A")
                .createStudentUserAccount(false)
                .build();

        StudentResponse result = conversionService.convertToStudent(303L, request, "principal_smith");

        assertNotNull(result);
        assertEquals(777L, result.getId());
        assertEquals("SA-2026-0007", result.getAdmissionNo());
        assertEquals("7", result.getRollNo()); // 6 + 1
        assertEquals("9", result.getStudentClass());
        assertEquals("A", result.getSection());

        // Verify Application transitioned to CONVERTED
        assertEquals(AdmissionStatus.CONVERTED, app.getStatus());
        assertEquals(777L, app.getConvertedStudentId());

        // Verify Guardian link and academic enrollment called
        verify(studentGuardianRepository).save(any());
        verify(academicServiceClient).enrollInActiveSession(
                eq(777L), anyString(), eq("SA-2026-0007"), eq("9"), eq("A"), eq("7"), eq("tenant-test")
        );
        verify(lifecycleLogRepository).save(argThat(log ->
                "ADMISSION_CONVERSION".equals(log.getAction()) && "principal_smith".equals(log.getActorUsername())));
    }
}
