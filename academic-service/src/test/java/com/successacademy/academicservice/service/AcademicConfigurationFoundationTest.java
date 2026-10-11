package com.successacademy.academicservice.service;

import com.successacademy.academicservice.dto.*;
import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.*;
import com.successacademy.academicservice.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicConfigurationFoundationTest {

    @Mock
    private AcademicClassRepository classRepository;

    @Mock
    private AcademicSectionRepository sectionRepository;

    @Mock
    private AcademicSubjectRepository subjectRepository;

    @Mock
    private AcademicClassSectionRepository classSectionRepository;

    @Mock
    private AcademicClassSubjectRepository classSubjectRepository;

    @Mock
    private AcademicSessionRepository sessionRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AcademicConfigurationService configService;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── 1 & 2. CREATE & RETRIEVE CLASSES FOR TENANT ──────────────────────────────

    @Test
    @DisplayName("Create class for Tenant A and retrieve only Tenant A classes")
    void testCreateAndRetrieveClasses() {
        TenantContext.setTenantId("tenant_a");

        when(classRepository.existsByTenantIdAndClassCode("tenant_a", "NURSERY")).thenReturn(false);
        when(classRepository.save(any(AcademicClass.class))).thenAnswer(inv -> {
            AcademicClass c = inv.getArgument(0);
            c.setId(101L);
            return c;
        });

        CreateAcademicClassRequest req = CreateAcademicClassRequest.builder()
                .classCode("NURSERY")
                .displayName("Nursery")
                .displayOrder(1)
                .category("PRE_PRIMARY")
                .status("ACTIVE")
                .build();

        AcademicClassDto created = configService.createClass(req, "tenant_a", 1L, "ADMIN");
        assertThat(created.getId()).isEqualTo(101L);
        assertThat(created.getClassCode()).isEqualTo("NURSERY");
        assertThat(created.getTenantId()).isEqualTo("tenant_a");

        verify(auditLogService).log(eq(1L), eq("ADMIN"), eq("CLASS_CREATED"), eq("AcademicClass"), eq(101L), any(), eq("NURSERY"), any());

        // Retrieve
        AcademicClass c1 = AcademicClass.builder().id(101L).tenantId("tenant_a").classCode("NURSERY").displayName("Nursery").displayOrder(1).status("ACTIVE").build();
        when(classRepository.findByTenantIdOrderByDisplayOrderAsc("tenant_a")).thenReturn(List.of(c1));

        List<AcademicClassDto> result = configService.getClasses("tenant_a", false);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClassCode()).isEqualTo("NURSERY");
    }

    // ── 3. TENANT ISOLATION & IDOR PROTECTION ────────────────────────────────────

    @Test
    @DisplayName("Tenant B cannot retrieve Tenant A class by ID (IDOR Protection -> 404)")
    void testTenantIsolation_IdorRejected() {
        TenantContext.setTenantId("tenant_b");

        // Class 101 belongs to tenant_a
        AcademicClass cTenantA = AcademicClass.builder().id(101L).tenantId("tenant_a").classCode("10").build();
        when(classRepository.findById(101L)).thenReturn(Optional.of(cTenantA));

        // When Tenant B tries to access Class 101
        assertThatThrownBy(() -> configService.getClassById(101L, "tenant_b"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Class not found for ID: 101");
    }

    // ── 4 & 5. SECTION CONFIGURATION & DUPLICATE VALIDATION ──────────────────────

    @Test
    @DisplayName("Duplicate section code within tenant is rejected with 409 Conflict")
    void testDuplicateSectionValidation() {
        TenantContext.setTenantId("tenant_a");
        when(sectionRepository.existsByTenantIdAndSectionCode("tenant_a", "A")).thenReturn(true);

        CreateAcademicSectionRequest req = CreateAcademicSectionRequest.builder()
                .sectionCode("A")
                .displayName("Section A")
                .build();

        assertThatThrownBy(() -> configService.createSection(req, "tenant_a", 1L, "ADMIN"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists for tenant");
    }

    // ── 6 & 7. SUBJECT CONFIGURATION & DUPLICATE VALIDATION ──────────────────────

    @Test
    @DisplayName("Create subject and duplicate subject code rejection")
    void testSubjectCreationAndDuplicateRejection() {
        TenantContext.setTenantId("tenant_a");

        when(subjectRepository.existsByTenantIdAndSubjectCode("tenant_a", "MATH")).thenReturn(false);
        when(subjectRepository.save(any(AcademicSubject.class))).thenAnswer(inv -> {
            AcademicSubject s = inv.getArgument(0);
            s.setId(201L);
            return s;
        });

        CreateAcademicSubjectRequest req = CreateAcademicSubjectRequest.builder()
                .subjectCode("MATH")
                .displayName("Mathematics")
                .subjectType("CORE")
                .displayOrder(1)
                .build();

        AcademicSubjectDto created = configService.createSubject(req, "tenant_a", 1L, "ADMIN");
        assertThat(created.getId()).isEqualTo(201L);
        assertThat(created.getSubjectCode()).isEqualTo("MATH");

        // Duplicate
        when(subjectRepository.existsByTenantIdAndSubjectCode("tenant_a", "MATH")).thenReturn(true);
        assertThatThrownBy(() -> configService.createSubject(req, "tenant_a", 1L, "ADMIN"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists for tenant");
    }

    // ── 8 & 9. SESSION-SPECIFIC OFFERINGS & INVALID TENANT RELATIONSHIP ─────────

    @Test
    @DisplayName("Add class-section offering rejected if session belongs to another tenant")
    void testSessionOffering_CrossTenantRejected() {
        TenantContext.setTenantId("tenant_a");

        // Session 5 belongs to tenant_b
        AcademicSession sessionTenantB = AcademicSession.builder().id(5L).tenantId("tenant_b").name("Session B").build();
        when(sessionRepository.findById(5L)).thenReturn(Optional.of(sessionTenantB));

        AcademicOfferingDto.CreateClassSectionOfferingRequest req = AcademicOfferingDto.CreateClassSectionOfferingRequest.builder()
                .classCode("10")
                .sectionCode("A")
                .build();

        assertThatThrownBy(() -> configService.addSessionClassSection(5L, req, "tenant_a", 1L, "ADMIN"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found for tenant: tenant_a");
    }

    // ── 10 & 11. INACTIVE ENTITY CANNOT BE USED FOR NEW OFFERING ─────────────────

    @Test
    @DisplayName("Inactive class or section cannot be used for new session offering")
    void testInactiveEntityCannotBeUsedForOffering() {
        TenantContext.setTenantId("tenant_a");

        AcademicSession validSession = AcademicSession.builder().id(3L).tenantId("tenant_a").build();
        when(sessionRepository.findById(3L)).thenReturn(Optional.of(validSession));

        // Class is INACTIVE
        AcademicClass inactiveClass = AcademicClass.builder().id(10L).tenantId("tenant_a").classCode("10").status("INACTIVE").build();
        when(classRepository.findByTenantIdAndClassCode("tenant_a", "10")).thenReturn(Optional.of(inactiveClass));

        AcademicOfferingDto.CreateClassSectionOfferingRequest req = AcademicOfferingDto.CreateClassSectionOfferingRequest.builder()
                .classCode("10")
                .sectionCode("A")
                .build();

        assertThatThrownBy(() -> configService.addSessionClassSection(3L, req, "tenant_a", 1L, "ADMIN"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("is not active or not found");
    }

    // ── 12. DYNAMIC PROGRESSION: TENANT WITH 1-10 CLASSES VS 1-12 CLASSES ─────────

    @Test
    @DisplayName("Dynamic Progression - In a 1-10 school, Class 10 graduates; In a 1-12 school, Class 12 graduates")
    void testDynamicProgressionEngine() {
        // Tenant DEF: Class 1 to 10
        TenantContext.setTenantId("school_def");
        List<AcademicClass> defClasses = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            defClasses.add(AcademicClass.builder().id((long) i).tenantId("school_def").classCode(String.valueOf(i)).displayOrder(i).status("ACTIVE").build());
        }
        when(classRepository.findByTenantIdAndStatusOrderByDisplayOrderAsc("school_def", "ACTIVE")).thenReturn(defClasses);

        // For school_def, Class 10 graduates!
        PromotionDecision decision10Def = configService.calculateSuggestedDecision("10", "school_def");
        assertThat(decision10Def).isEqualTo(PromotionDecision.GRADUATE);

        String nextClass10Def = configService.calculateNextClass("10", "school_def", PromotionDecision.PROMOTE);
        assertThat(nextClass10Def).isEqualTo("GRADUATED");

        // Class 9 promotes to Class 10
        PromotionDecision decision9Def = configService.calculateSuggestedDecision("9", "school_def");
        assertThat(decision9Def).isEqualTo(PromotionDecision.PROMOTE);

        String nextClass9Def = configService.calculateNextClass("9", "school_def", PromotionDecision.PROMOTE);
        assertThat(nextClass9Def).isEqualTo("10");

        // Tenant ABC: Nursery to 12
        TenantContext.setTenantId("school_abc");
        List<AcademicClass> abcClasses = List.of(
                AcademicClass.builder().classCode("NURSERY").displayOrder(1).status("ACTIVE").build(),
                AcademicClass.builder().classCode("LKG").displayOrder(2).status("ACTIVE").build(),
                AcademicClass.builder().classCode("UKG").displayOrder(3).status("ACTIVE").build(),
                AcademicClass.builder().classCode("1").displayOrder(4).status("ACTIVE").build(),
                AcademicClass.builder().classCode("10").displayOrder(13).status("ACTIVE").build(),
                AcademicClass.builder().classCode("11").displayOrder(14).status("ACTIVE").build(),
                AcademicClass.builder().classCode("12").displayOrder(15).status("ACTIVE").build()
        );
        when(classRepository.findByTenantIdAndStatusOrderByDisplayOrderAsc("school_abc", "ACTIVE")).thenReturn(abcClasses);

        // For school_abc, Class 10 DOES NOT graduate - promotes to 11!
        PromotionDecision decision10Abc = configService.calculateSuggestedDecision("10", "school_abc");
        assertThat(decision10Abc).isEqualTo(PromotionDecision.PROMOTE);

        String nextClass10Abc = configService.calculateNextClass("10", "school_abc", PromotionDecision.PROMOTE);
        assertThat(nextClass10Abc).isEqualTo("11");

        // Class 12 graduates!
        PromotionDecision decision12Abc = configService.calculateSuggestedDecision("12", "school_abc");
        assertThat(decision12Abc).isEqualTo(PromotionDecision.GRADUATE);

        // Nursery promotes to LKG
        String nextNursery = configService.calculateNextClass("NURSERY", "school_abc", PromotionDecision.PROMOTE);
        assertThat(nextNursery).isEqualTo("LKG");
    }

    // ── 13. STRUCTURE AGGREGATION ────────────────────────────────────────────────

    @Test
    @DisplayName("Aggregate structure returns active classes, sections, and subjects in one response")
    void testGetAcademicStructure() {
        TenantContext.setTenantId("tenant_a");

        when(classRepository.findByTenantIdAndStatusOrderByDisplayOrderAsc("tenant_a", "ACTIVE"))
                .thenReturn(List.of(AcademicClass.builder().id(1L).tenantId("tenant_a").classCode("1").displayName("Class 1").status("ACTIVE").build()));
        when(sectionRepository.findByTenantIdAndStatusOrderByDisplayOrderAsc("tenant_a", "ACTIVE"))
                .thenReturn(List.of(AcademicSection.builder().id(1L).tenantId("tenant_a").sectionCode("A").displayName("Section A").status("ACTIVE").build()));
        when(subjectRepository.findByTenantIdAndStatusOrderByDisplayOrderAsc("tenant_a", "ACTIVE"))
                .thenReturn(List.of(AcademicSubject.builder().id(1L).tenantId("tenant_a").subjectCode("MATH").displayName("Mathematics").status("ACTIVE").build()));

        AcademicStructureResponse structure = configService.getAcademicStructure("tenant_a");
        assertThat(structure.getTenantId()).isEqualTo("tenant_a");
        assertThat(structure.getClasses()).hasSize(1);
        assertThat(structure.getSections()).hasSize(1);
        assertThat(structure.getSubjects()).hasSize(1);
    }
}
