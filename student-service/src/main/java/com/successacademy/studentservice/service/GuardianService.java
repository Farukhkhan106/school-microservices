package com.successacademy.studentservice.service;

import com.successacademy.studentservice.dto.*;
import com.successacademy.studentservice.model.*;
import com.successacademy.studentservice.repository.*;
import com.successacademy.studentservice.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public GuardianResponse createGuardian(GuardianRequest request) {
        String tenantId = TenantContext.getTenantId();

        String displayName = (request.getFirstName() + " " + (request.getLastName() != null ? request.getLastName() : "")).trim();

        Guardian guardian = Guardian.builder()
                .tenantId(tenantId)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName() != null ? request.getLastName().trim() : null)
                .displayName(displayName)
                .phone(request.getPhone().trim())
                .alternatePhone(request.getAlternatePhone() != null ? request.getAlternatePhone().trim() : null)
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .address(request.getAddress())
                .occupation(request.getOccupation())
                .relationshipMetadata(request.getRelationshipMetadata())
                .status("ACTIVE")
                .build();

        Guardian saved = guardianRepository.save(guardian);
        log.info("Created guardian id={} displayName={} tenant={}", saved.getId(), saved.getDisplayName(), tenantId);
        return mapToResponse(saved);
    }

    @Transactional
    public GuardianResponse updateGuardian(Long guardianId, GuardianRequest request) {
        String tenantId = TenantContext.getTenantId();
        Guardian guardian = guardianRepository.findByIdAndTenantId(guardianId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guardian not found"));

        guardian.setFirstName(request.getFirstName().trim());
        guardian.setLastName(request.getLastName() != null ? request.getLastName().trim() : null);
        guardian.setDisplayName((request.getFirstName() + " " + (request.getLastName() != null ? request.getLastName() : "")).trim());
        guardian.setPhone(request.getPhone().trim());
        guardian.setAlternatePhone(request.getAlternatePhone() != null ? request.getAlternatePhone().trim() : null);
        guardian.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        guardian.setAddress(request.getAddress());
        guardian.setOccupation(request.getOccupation());
        guardian.setRelationshipMetadata(request.getRelationshipMetadata());

        Guardian saved = guardianRepository.save(guardian);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public GuardianResponse getGuardian(Long guardianId) {
        String tenantId = TenantContext.getTenantId();
        Guardian guardian = guardianRepository.findByIdAndTenantId(guardianId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guardian not found"));
        return mapToResponse(guardian);
    }

    @Transactional(readOnly = true)
    public Page<GuardianResponse> listGuardians(String query, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        Page<Guardian> page;
        if (query != null && !query.isBlank()) {
            page = guardianRepository.searchGuardians(tenantId, query.trim(), pageable);
        } else {
            page = guardianRepository.findByTenantId(tenantId, pageable);
        }
        return page.map(this::mapToResponse);
    }

    @Transactional
    public StudentGuardianDto linkGuardianToStudent(Long studentId, StudentGuardianLinkRequest request) {
        String tenantId = TenantContext.getTenantId();
        Student student = studentRepository.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        Guardian guardian;
        if (request.getGuardianId() != null) {
            guardian = guardianRepository.findByIdAndTenantId(request.getGuardianId(), tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guardian not found for tenant"));
        } else if (request.getInlineGuardian() != null) {
            GuardianResponse created = createGuardian(request.getInlineGuardian());
            guardian = guardianRepository.findByIdAndTenantId(created.getId(), tenantId).orElseThrow();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either guardianId or inlineGuardian must be provided");
        }

        // Check if already linked
        StudentGuardian link = studentGuardianRepository
                .findByStudentIdAndGuardianIdAndTenantId(studentId, guardian.getId(), tenantId)
                .orElseGet(() -> StudentGuardian.builder()
                        .studentId(studentId)
                        .guardianId(guardian.getId())
                        .tenantId(tenantId)
                        .build());

        link.setRelationshipType(request.getRelationshipType() != null ? request.getRelationshipType() : GuardianRelationship.GUARDIAN);
        link.setIsPrimary(Boolean.TRUE.equals(request.getIsPrimary()));
        link.setIsEmergencyContact(Boolean.TRUE.equals(request.getIsEmergencyContact()));
        link.setCanPickup(Boolean.TRUE.equals(request.getCanPickup()));
        link.setCanReceiveNotifications(Boolean.TRUE.equals(request.getCanReceiveNotifications()));
        link.setCanAccessPortal(Boolean.TRUE.equals(request.getCanAccessPortal()));
        link.setStatus("ACTIVE");

        StudentGuardian saved = studentGuardianRepository.save(link);
        log.info("Linked guardian {} to student {} as {} for tenant {}",
                guardian.getId(), studentId, saved.getRelationshipType(), tenantId);

        return mapToDto(saved, guardian);
    }

    @Transactional
    public void unlinkGuardianFromStudent(Long studentId, Long guardianId) {
        String tenantId = TenantContext.getTenantId();
        StudentGuardian link = studentGuardianRepository
                .findByStudentIdAndGuardianIdAndTenantId(studentId, guardianId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guardian relationship not found"));

        studentGuardianRepository.delete(link);
        log.info("Unlinked guardian {} from student {} for tenant {}", guardianId, studentId, tenantId);
    }

    @Transactional(readOnly = true)
    public List<StudentGuardianDto> getGuardiansForStudent(Long studentId) {
        String tenantId = TenantContext.getTenantId();
        List<StudentGuardian> links = studentGuardianRepository.findByStudentIdAndTenantId(studentId, tenantId);
        return links.stream().map(link -> {
            Guardian g = guardianRepository.findByIdAndTenantId(link.getGuardianId(), tenantId).orElse(null);
            return mapToDto(link, g);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isGuardianLinkedToStudent(Long guardianId, Long studentId, String tenantId) {
        if (guardianId == null || studentId == null) return false;
        return studentGuardianRepository.findByStudentIdAndGuardianIdAndTenantId(studentId, guardianId, tenantId).isPresent();
    }

    public GuardianResponse mapToResponse(Guardian g) {
        if (g == null) return null;
        return GuardianResponse.builder()
                .id(g.getId())
                .tenantId(g.getTenantId())
                .userId(g.getUserId())
                .firstName(g.getFirstName())
                .lastName(g.getLastName())
                .displayName(g.getDisplayName())
                .phone(g.getPhone())
                .alternatePhone(g.getAlternatePhone())
                .email(g.getEmail())
                .address(g.getAddress())
                .occupation(g.getOccupation())
                .relationshipMetadata(g.getRelationshipMetadata())
                .status(g.getStatus())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }

    private StudentGuardianDto mapToDto(StudentGuardian link, Guardian g) {
        return StudentGuardianDto.builder()
                .id(link.getId())
                .studentId(link.getStudentId())
                .guardianId(link.getGuardianId())
                .guardian(mapToResponse(g))
                .relationshipType(link.getRelationshipType())
                .isPrimary(link.getIsPrimary())
                .isEmergencyContact(link.getIsEmergencyContact())
                .canPickup(link.getCanPickup())
                .canReceiveNotifications(link.getCanReceiveNotifications())
                .canAccessPortal(link.getCanAccessPortal())
                .status(link.getStatus())
                .createdAt(link.getCreatedAt())
                .build();
    }
}
