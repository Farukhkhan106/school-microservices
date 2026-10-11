package com.successacademy.academicservice.service;

import com.successacademy.academicservice.dto.*;
import com.successacademy.academicservice.model.*;
import com.successacademy.academicservice.repository.*;
import com.successacademy.academicservice.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicConfigurationService {

    private final AcademicClassRepository classRepository;
    private final AcademicSectionRepository sectionRepository;
    private final AcademicSubjectRepository subjectRepository;
    private final AcademicClassSectionRepository classSectionRepository;
    private final AcademicClassSubjectRepository classSubjectRepository;
    private final AcademicSessionRepository sessionRepository;
    private final AuditLogService auditLogService;

    private String resolveTenant(String tenantId) {
        String ctxTenant = TenantContext.getTenantId();
        if (ctxTenant != null && !ctxTenant.isBlank()) {
            return ctxTenant.trim();
        }
        return (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
    }

    // ====================================================================
    // 1. ACADEMIC CLASSES / GRADES
    // ====================================================================

    @Transactional(readOnly = true)
    public List<AcademicClassDto> getClasses(String tenantId, boolean activeOnly) {
        String cleanTenant = resolveTenant(tenantId);
        List<AcademicClass> list = activeOnly
                ? classRepository.findByTenantIdAndStatusOrderByDisplayOrderAsc(cleanTenant, "ACTIVE")
                : classRepository.findByTenantIdOrderByDisplayOrderAsc(cleanTenant);
        return list.stream().map(this::mapToClassDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AcademicClassDto getClassById(Long id, String tenantId) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicClass ac = classRepository.findById(id)
                .filter(c -> cleanTenant.equalsIgnoreCase(c.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found for ID: " + id));
        return mapToClassDto(ac);
    }

    @Transactional
    public AcademicClassDto createClass(CreateAcademicClassRequest req, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        String cleanCode = req.getClassCode().trim().toUpperCase();

        if (classRepository.existsByTenantIdAndClassCode(cleanTenant, cleanCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Class with code '" + cleanCode + "' already exists for tenant.");
        }

        AcademicClass ac = AcademicClass.builder()
                .tenantId(cleanTenant)
                .classCode(cleanCode)
                .displayName(req.getDisplayName().trim())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .category(req.getCategory() != null ? req.getCategory().trim().toUpperCase() : "PRIMARY")
                .status(req.getStatus() != null ? req.getStatus().trim().toUpperCase() : "ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        AcademicClass saved = classRepository.save(ac);
        auditLogService.log(actorUserId, actorRole, "CLASS_CREATED", "AcademicClass",
                saved.getId(), null, saved.getClassCode(), "Created class: " + saved.getDisplayName());

        return mapToClassDto(saved);
    }

    @Transactional
    public AcademicClassDto updateClass(Long id, CreateAcademicClassRequest req, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicClass ac = classRepository.findById(id)
                .filter(c -> cleanTenant.equalsIgnoreCase(c.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found for ID: " + id));

        String cleanCode = req.getClassCode().trim().toUpperCase();
        if (classRepository.existsByTenantIdAndClassCodeAndIdNot(cleanTenant, cleanCode, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another class with code '" + cleanCode + "' already exists.");
        }

        ac.setClassCode(cleanCode);
        ac.setDisplayName(req.getDisplayName().trim());
        if (req.getDisplayOrder() != null) ac.setDisplayOrder(req.getDisplayOrder());
        if (req.getCategory() != null) ac.setCategory(req.getCategory().trim().toUpperCase());
        if (req.getStatus() != null) ac.setStatus(req.getStatus().trim().toUpperCase());
        ac.setUpdatedAt(LocalDateTime.now());

        AcademicClass saved = classRepository.save(ac);
        auditLogService.log(actorUserId, actorRole, "CLASS_UPDATED", "AcademicClass",
                saved.getId(), null, saved.getClassCode(), "Updated class: " + saved.getDisplayName());

        return mapToClassDto(saved);
    }

    @Transactional
    public AcademicClassDto updateClassStatus(Long id, String status, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicClass ac = classRepository.findById(id)
                .filter(c -> cleanTenant.equalsIgnoreCase(c.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found for ID: " + id));

        String oldStatus = ac.getStatus();
        ac.setStatus(status.trim().toUpperCase());
        ac.setUpdatedAt(LocalDateTime.now());
        AcademicClass saved = classRepository.save(ac);

        auditLogService.log(actorUserId, actorRole, "CLASS_STATUS_CHANGED", "AcademicClass",
                saved.getId(), oldStatus, saved.getStatus(), "Changed status for class: " + saved.getClassCode());

        return mapToClassDto(saved);
    }

    // ====================================================================
    // 2. ACADEMIC SECTIONS
    // ====================================================================

    @Transactional(readOnly = true)
    public List<AcademicSectionDto> getSections(String tenantId, boolean activeOnly) {
        String cleanTenant = resolveTenant(tenantId);
        List<AcademicSection> list = activeOnly
                ? sectionRepository.findByTenantIdAndStatusOrderByDisplayOrderAsc(cleanTenant, "ACTIVE")
                : sectionRepository.findByTenantIdOrderByDisplayOrderAsc(cleanTenant);
        return list.stream().map(this::mapToSectionDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AcademicSectionDto getSectionById(Long id, String tenantId) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSection as = sectionRepository.findById(id)
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found for ID: " + id));
        return mapToSectionDto(as);
    }

    @Transactional
    public AcademicSectionDto createSection(CreateAcademicSectionRequest req, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        String cleanCode = req.getSectionCode().trim().toUpperCase();

        if (sectionRepository.existsByTenantIdAndSectionCode(cleanTenant, cleanCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Section with code '" + cleanCode + "' already exists for tenant.");
        }

        AcademicSection as = AcademicSection.builder()
                .tenantId(cleanTenant)
                .sectionCode(cleanCode)
                .displayName(req.getDisplayName().trim())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .status(req.getStatus() != null ? req.getStatus().trim().toUpperCase() : "ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        AcademicSection saved = sectionRepository.save(as);
        auditLogService.log(actorUserId, actorRole, "SECTION_CREATED", "AcademicSection",
                saved.getId(), null, saved.getSectionCode(), "Created section: " + saved.getDisplayName());

        return mapToSectionDto(saved);
    }

    @Transactional
    public AcademicSectionDto updateSection(Long id, CreateAcademicSectionRequest req, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSection as = sectionRepository.findById(id)
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found for ID: " + id));

        String cleanCode = req.getSectionCode().trim().toUpperCase();
        if (sectionRepository.existsByTenantIdAndSectionCodeAndIdNot(cleanTenant, cleanCode, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another section with code '" + cleanCode + "' already exists.");
        }

        as.setSectionCode(cleanCode);
        as.setDisplayName(req.getDisplayName().trim());
        if (req.getDisplayOrder() != null) as.setDisplayOrder(req.getDisplayOrder());
        if (req.getStatus() != null) as.setStatus(req.getStatus().trim().toUpperCase());
        as.setUpdatedAt(LocalDateTime.now());

        AcademicSection saved = sectionRepository.save(as);
        auditLogService.log(actorUserId, actorRole, "SECTION_UPDATED", "AcademicSection",
                saved.getId(), null, saved.getSectionCode(), "Updated section: " + saved.getDisplayName());

        return mapToSectionDto(saved);
    }

    @Transactional
    public AcademicSectionDto updateSectionStatus(Long id, String status, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSection as = sectionRepository.findById(id)
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found for ID: " + id));

        String oldStatus = as.getStatus();
        as.setStatus(status.trim().toUpperCase());
        as.setUpdatedAt(LocalDateTime.now());
        AcademicSection saved = sectionRepository.save(as);

        auditLogService.log(actorUserId, actorRole, "SECTION_STATUS_CHANGED", "AcademicSection",
                saved.getId(), oldStatus, saved.getStatus(), "Changed status for section: " + saved.getSectionCode());

        return mapToSectionDto(saved);
    }

    // ====================================================================
    // 3. ACADEMIC SUBJECTS
    // ====================================================================

    @Transactional(readOnly = true)
    public List<AcademicSubjectDto> getSubjects(String tenantId, boolean activeOnly) {
        String cleanTenant = resolveTenant(tenantId);
        List<AcademicSubject> list = activeOnly
                ? subjectRepository.findByTenantIdAndStatusOrderByDisplayOrderAsc(cleanTenant, "ACTIVE")
                : subjectRepository.findByTenantIdOrderByDisplayOrderAsc(cleanTenant);
        return list.stream().map(this::mapToSubjectDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AcademicSubjectDto getSubjectById(Long id, String tenantId) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSubject as = subjectRepository.findById(id)
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found for ID: " + id));
        return mapToSubjectDto(as);
    }

    @Transactional
    public AcademicSubjectDto createSubject(CreateAcademicSubjectRequest req, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        String cleanCode = req.getSubjectCode().trim().toUpperCase();

        if (subjectRepository.existsByTenantIdAndSubjectCode(cleanTenant, cleanCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject with code '" + cleanCode + "' already exists for tenant.");
        }

        AcademicSubject as = AcademicSubject.builder()
                .tenantId(cleanTenant)
                .subjectCode(cleanCode)
                .displayName(req.getDisplayName().trim())
                .subjectType(req.getSubjectType() != null ? req.getSubjectType().trim().toUpperCase() : "CORE")
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .status(req.getStatus() != null ? req.getStatus().trim().toUpperCase() : "ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        AcademicSubject saved = subjectRepository.save(as);
        auditLogService.log(actorUserId, actorRole, "SUBJECT_CREATED", "AcademicSubject",
                saved.getId(), null, saved.getSubjectCode(), "Created subject: " + saved.getDisplayName());

        return mapToSubjectDto(saved);
    }

    @Transactional
    public AcademicSubjectDto updateSubject(Long id, CreateAcademicSubjectRequest req, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSubject as = subjectRepository.findById(id)
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found for ID: " + id));

        String cleanCode = req.getSubjectCode().trim().toUpperCase();
        if (subjectRepository.existsByTenantIdAndSubjectCodeAndIdNot(cleanTenant, cleanCode, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another subject with code '" + cleanCode + "' already exists.");
        }

        as.setSubjectCode(cleanCode);
        as.setDisplayName(req.getDisplayName().trim());
        if (req.getSubjectType() != null) as.setSubjectType(req.getSubjectType().trim().toUpperCase());
        if (req.getDisplayOrder() != null) as.setDisplayOrder(req.getDisplayOrder());
        if (req.getStatus() != null) as.setStatus(req.getStatus().trim().toUpperCase());
        as.setUpdatedAt(LocalDateTime.now());

        AcademicSubject saved = subjectRepository.save(as);
        auditLogService.log(actorUserId, actorRole, "SUBJECT_UPDATED", "AcademicSubject",
                saved.getId(), null, saved.getSubjectCode(), "Updated subject: " + saved.getDisplayName());

        return mapToSubjectDto(saved);
    }

    @Transactional
    public AcademicSubjectDto updateSubjectStatus(Long id, String status, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicSubject as = subjectRepository.findById(id)
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found for ID: " + id));

        String oldStatus = as.getStatus();
        as.setStatus(status.trim().toUpperCase());
        as.setUpdatedAt(LocalDateTime.now());
        AcademicSubject saved = subjectRepository.save(as);

        auditLogService.log(actorUserId, actorRole, "SUBJECT_STATUS_CHANGED", "AcademicSubject",
                saved.getId(), oldStatus, saved.getStatus(), "Changed status for subject: " + saved.getSubjectCode());

        return mapToSubjectDto(saved);
    }

    // ====================================================================
    // 4. STRUCTURE AGGREGATION (FOR HIGH PERFORMANCE CLIENT RENDERING)
    // ====================================================================

    @Transactional(readOnly = true)
    public AcademicStructureResponse getAcademicStructure(String tenantId) {
        String cleanTenant = resolveTenant(tenantId);
        return AcademicStructureResponse.builder()
                .tenantId(cleanTenant)
                .classes(getClasses(cleanTenant, true))
                .sections(getSections(cleanTenant, true))
                .subjects(getSubjects(cleanTenant, true))
                .build();
    }

    // ====================================================================
    // 5. SESSION-SPECIFIC ACADEMIC OFFERINGS
    // ====================================================================

    @Transactional(readOnly = true)
    public List<AcademicOfferingDto.ClassSectionOfferingDto> getSessionClassSections(Long sessionId, String tenantId) {
        String cleanTenant = resolveTenant(tenantId);
        validateSessionTenant(sessionId, cleanTenant);

        return classSectionRepository.findByTenantIdAndSessionId(cleanTenant, sessionId).stream()
                .map(this::mapToClassSectionOfferingDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AcademicOfferingDto.ClassSectionOfferingDto addSessionClassSection(
            Long sessionId, AcademicOfferingDto.CreateClassSectionOfferingRequest req,
            String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        validateSessionTenant(sessionId, cleanTenant);

        String cleanClass = req.getClassCode().trim().toUpperCase();
        String cleanSection = req.getSectionCode().trim().toUpperCase();

        // Validate class exists and is ACTIVE for tenant
        classRepository.findByTenantIdAndClassCode(cleanTenant, cleanClass)
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class '" + cleanClass + "' is not active or not found for tenant."));

        // Validate section exists and is ACTIVE for tenant
        sectionRepository.findByTenantIdAndSectionCode(cleanTenant, cleanSection)
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section '" + cleanSection + "' is not active or not found for tenant."));

        if (classSectionRepository.existsByTenantIdAndSessionIdAndClassCodeAndSectionCode(cleanTenant, sessionId, cleanClass, cleanSection)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Offering for class " + cleanClass + " section " + cleanSection + " already exists in session.");
        }

        AcademicClassSection acs = AcademicClassSection.builder()
                .tenantId(cleanTenant)
                .sessionId(sessionId)
                .classCode(cleanClass)
                .sectionCode(cleanSection)
                .capacity(req.getCapacity() != null ? req.getCapacity() : 40)
                .roomNumber(req.getRoomNumber())
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        AcademicClassSection saved = classSectionRepository.save(acs);
        auditLogService.log(actorUserId, actorRole, "OFFERING_CLASS_SECTION_ADDED", "AcademicClassSection",
                saved.getId(), null, cleanClass + "-" + cleanSection, "Added class-section offering to session " + sessionId);

        return mapToClassSectionOfferingDto(saved);
    }

    @Transactional
    public void removeSessionClassSection(Long id, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicClassSection acs = classSectionRepository.findById(id)
                .filter(a -> cleanTenant.equalsIgnoreCase(a.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offering not found."));

        classSectionRepository.delete(acs);
        auditLogService.log(actorUserId, actorRole, "OFFERING_CLASS_SECTION_REMOVED", "AcademicClassSection",
                acs.getId(), null, acs.getClassCode() + "-" + acs.getSectionCode(), "Removed class-section offering");
    }

    @Transactional(readOnly = true)
    public List<AcademicOfferingDto.ClassSubjectOfferingDto> getSessionClassSubjects(Long sessionId, String tenantId) {
        String cleanTenant = resolveTenant(tenantId);
        validateSessionTenant(sessionId, cleanTenant);

        return classSubjectRepository.findByTenantIdAndSessionId(cleanTenant, sessionId).stream()
                .map(this::mapToClassSubjectOfferingDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AcademicOfferingDto.ClassSubjectOfferingDto addSessionClassSubject(
            Long sessionId, AcademicOfferingDto.CreateClassSubjectOfferingRequest req,
            String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        validateSessionTenant(sessionId, cleanTenant);

        String cleanClass = req.getClassCode().trim().toUpperCase();
        String cleanSubject = req.getSubjectCode().trim().toUpperCase();

        classRepository.findByTenantIdAndClassCode(cleanTenant, cleanClass)
                .filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class '" + cleanClass + "' is not active or not found."));

        subjectRepository.findByTenantIdAndSubjectCode(cleanTenant, cleanSubject)
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject '" + cleanSubject + "' is not active or not found."));

        if (classSubjectRepository.existsByTenantIdAndSessionIdAndClassCodeAndSubjectCode(cleanTenant, sessionId, cleanClass, cleanSubject)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Subject offering for class " + cleanClass + " subject " + cleanSubject + " already exists in session.");
        }

        AcademicClassSubject acsubj = AcademicClassSubject.builder()
                .tenantId(cleanTenant)
                .sessionId(sessionId)
                .classCode(cleanClass)
                .subjectCode(cleanSubject)
                .isMandatory(req.getIsMandatory() != null ? req.getIsMandatory() : true)
                .weeklyPeriods(req.getWeeklyPeriods() != null ? req.getWeeklyPeriods() : 5)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        AcademicClassSubject saved = classSubjectRepository.save(acsubj);
        auditLogService.log(actorUserId, actorRole, "OFFERING_CLASS_SUBJECT_ADDED", "AcademicClassSubject",
                saved.getId(), null, cleanClass + "-" + cleanSubject, "Added subject offering to session " + sessionId);

        return mapToClassSubjectOfferingDto(saved);
    }

    @Transactional
    public void removeSessionClassSubject(Long id, String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = resolveTenant(tenantId);
        AcademicClassSubject acsubj = classSubjectRepository.findById(id)
                .filter(a -> cleanTenant.equalsIgnoreCase(a.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offering not found."));

        classSubjectRepository.delete(acsubj);
        auditLogService.log(actorUserId, actorRole, "OFFERING_CLASS_SUBJECT_REMOVED", "AcademicClassSubject",
                acsubj.getId(), null, acsubj.getClassCode() + "-" + acsubj.getSubjectCode(), "Removed subject offering");
    }

    // ====================================================================
    // 6. DYNAMIC PROGRESSION ENGINE (ZERO HARDCODED CLASSES)
    // ====================================================================

    @Transactional(readOnly = true)
    public PromotionDecision calculateSuggestedDecision(String currentClass, String tenantId) {
        if (currentClass == null || currentClass.isBlank()) return PromotionDecision.PROMOTE;
        String cleanTenant = resolveTenant(tenantId);
        String clean = currentClass.trim().toUpperCase();

        List<AcademicClass> classes = classRepository.findByTenantIdAndStatusOrderByDisplayOrderAsc(cleanTenant, "ACTIVE");
        if (classes.isEmpty()) {
            // Fallback if tenant classes haven't been seeded
            if ("12".equals(clean) || "CLASS 12".equals(clean) || "XII".equals(clean)) {
                return PromotionDecision.GRADUATE;
            }
            return PromotionDecision.PROMOTE;
        }

        AcademicClass highestClass = classes.get(classes.size() - 1);
        if (highestClass.getClassCode().equalsIgnoreCase(clean) ||
            (highestClass.getDisplayName() != null && highestClass.getDisplayName().equalsIgnoreCase(clean))) {
            return PromotionDecision.GRADUATE;
        }

        return PromotionDecision.PROMOTE;
    }

    @Transactional(readOnly = true)
    public String calculateNextClass(String currentClass, String tenantId, PromotionDecision decision) {
        if (decision == PromotionDecision.GRADUATE) {
            return "GRADUATED";
        }
        if (decision == PromotionDecision.RETAIN) {
            return currentClass;
        }
        if (currentClass == null || currentClass.isBlank()) {
            return "1";
        }

        String cleanTenant = resolveTenant(tenantId);
        String clean = currentClass.trim().toUpperCase();

        List<AcademicClass> classes = classRepository.findByTenantIdAndStatusOrderByDisplayOrderAsc(cleanTenant, "ACTIVE");
        if (!classes.isEmpty()) {
            for (int i = 0; i < classes.size(); i++) {
                AcademicClass ac = classes.get(i);
                if (ac.getClassCode().equalsIgnoreCase(clean) ||
                    (ac.getDisplayName() != null && ac.getDisplayName().equalsIgnoreCase(clean))) {
                    if (i + 1 < classes.size()) {
                        return classes.get(i + 1).getClassCode();
                    } else {
                        return "GRADUATED";
                    }
                }
            }
        }

        // Graceful numerical fallback if class code is a number
        try {
            int num = Integer.parseInt(clean);
            return String.valueOf(num + 1);
        } catch (NumberFormatException ignored) {}

        return currentClass;
    }

    private void validateSessionTenant(Long sessionId, String cleanTenant) {
        sessionRepository.findById(sessionId)
                .filter(s -> cleanTenant.equalsIgnoreCase(s.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic session " + sessionId + " not found for tenant: " + cleanTenant));
    }

    // ====================================================================
    // MAPPERS
    // ====================================================================

    private AcademicClassDto mapToClassDto(AcademicClass c) {
        return AcademicClassDto.builder()
                .id(c.getId())
                .tenantId(c.getTenantId())
                .classCode(c.getClassCode())
                .displayName(c.getDisplayName())
                .displayOrder(c.getDisplayOrder())
                .category(c.getCategory())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private AcademicSectionDto mapToSectionDto(AcademicSection s) {
        return AcademicSectionDto.builder()
                .id(s.getId())
                .tenantId(s.getTenantId())
                .sectionCode(s.getSectionCode())
                .displayName(s.getDisplayName())
                .displayOrder(s.getDisplayOrder())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private AcademicSubjectDto mapToSubjectDto(AcademicSubject sub) {
        return AcademicSubjectDto.builder()
                .id(sub.getId())
                .tenantId(sub.getTenantId())
                .subjectCode(sub.getSubjectCode())
                .displayName(sub.getDisplayName())
                .subjectType(sub.getSubjectType())
                .displayOrder(sub.getDisplayOrder())
                .status(sub.getStatus())
                .createdAt(sub.getCreatedAt())
                .updatedAt(sub.getUpdatedAt())
                .build();
    }

    private AcademicOfferingDto.ClassSectionOfferingDto mapToClassSectionOfferingDto(AcademicClassSection acs) {
        return AcademicOfferingDto.ClassSectionOfferingDto.builder()
                .id(acs.getId())
                .tenantId(acs.getTenantId())
                .sessionId(acs.getSessionId())
                .classCode(acs.getClassCode())
                .sectionCode(acs.getSectionCode())
                .capacity(acs.getCapacity())
                .roomNumber(acs.getRoomNumber())
                .status(acs.getStatus())
                .createdAt(acs.getCreatedAt())
                .build();
    }

    private AcademicOfferingDto.ClassSubjectOfferingDto mapToClassSubjectOfferingDto(AcademicClassSubject acsubj) {
        return AcademicOfferingDto.ClassSubjectOfferingDto.builder()
                .id(acsubj.getId())
                .tenantId(acsubj.getTenantId())
                .sessionId(acsubj.getSessionId())
                .classCode(acsubj.getClassCode())
                .subjectCode(acsubj.getSubjectCode())
                .isMandatory(acsubj.isMandatory())
                .weeklyPeriods(acsubj.getWeeklyPeriods())
                .status(acsubj.getStatus())
                .createdAt(acsubj.getCreatedAt())
                .build();
    }
}
