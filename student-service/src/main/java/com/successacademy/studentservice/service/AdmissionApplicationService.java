package com.successacademy.studentservice.service;

import com.successacademy.studentservice.dto.*;
import com.successacademy.studentservice.model.*;
import com.successacademy.studentservice.repository.AdmissionApplicationRepository;
import com.successacademy.studentservice.repository.AdmissionDocumentRepository;
import com.successacademy.studentservice.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdmissionApplicationService {

    private final AdmissionApplicationRepository applicationRepository;
    private final AdmissionDocumentRepository documentRepository;
    private final AdmissionSequenceService sequenceService;
    private final AdmissionDocumentService documentService;

    @Transactional
    public AdmissionApplicationResponse createApplication(AdmissionApplicationRequest request, String createdBy, boolean autoSubmit) {
        String tenantId = TenantContext.getTenantId();
        String appNumber = sequenceService.nextApplicationNumber(tenantId);

        AdmissionStatus initialStatus = autoSubmit ? AdmissionStatus.SUBMITTED : AdmissionStatus.DRAFT;

        AdmissionApplication app = AdmissionApplication.builder()
                .tenantId(tenantId)
                .applicationNumber(appNumber)
                .academicSessionId(request.getAcademicSessionId())
                .academicSessionCode(request.getAcademicSessionCode())
                .applyingClass(request.getApplyingClass().trim())
                .stream(request.getStream())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName() != null ? request.getLastName().trim() : "")
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .bloodGroup(request.getBloodGroup())
                .nationality(request.getNationality())
                .religion(request.getReligion())
                .category(request.getCategory())
                .previousSchoolName(request.getPreviousSchoolName())
                .previousSchoolClass(request.getPreviousSchoolClass())
                .previousSchoolPercentage(request.getPreviousSchoolPercentage())
                .phone(request.getPhone())
                .email(request.getEmail())
                .alternatePhone(request.getAlternatePhone())
                .fatherName(request.getFatherName())
                .fatherPhone(request.getFatherPhone())
                .fatherEmail(request.getFatherEmail())
                .fatherOccupation(request.getFatherOccupation())
                .motherName(request.getMotherName())
                .motherPhone(request.getMotherPhone())
                .motherEmail(request.getMotherEmail())
                .motherOccupation(request.getMotherOccupation())
                .guardianName(request.getGuardianName())
                .guardianPhone(request.getGuardianPhone())
                .guardianEmail(request.getGuardianEmail())
                .guardianRelation(request.getGuardianRelation())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .country(request.getCountry())
                .status(initialStatus)
                .source(request.getSource() != null ? request.getSource() : "PORTAL")
                .notes(request.getRemarks())
                .createdBy(createdBy != null ? createdBy : "system")
                .submittedAt(autoSubmit ? LocalDateTime.now() : null)
                .build();

        AdmissionApplication saved = applicationRepository.save(app);
        log.info("Created admission application id={} appNo={} status={} tenant={}",
                saved.getId(), saved.getApplicationNumber(), saved.getStatus(), tenantId);

        return mapToResponse(saved);
    }

    @Transactional
    public AdmissionApplicationResponse updateApplication(Long id, AdmissionApplicationRequest request, String updatedBy) {
        String tenantId = TenantContext.getTenantId();
        AdmissionApplication app = applicationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission application not found"));

        if (app.getStatus() != AdmissionStatus.DRAFT && app.getStatus() != AdmissionStatus.DOCUMENTS_REQUIRED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Application in status " + app.getStatus() + " cannot be modified");
        }

        app.setAcademicSessionId(request.getAcademicSessionId());
        app.setApplyingClass(request.getApplyingClass().trim());
        app.setStream(request.getStream());
        app.setFirstName(request.getFirstName().trim());
        app.setLastName(request.getLastName() != null ? request.getLastName().trim() : "");
        app.setDateOfBirth(request.getDateOfBirth());
        app.setGender(request.getGender());
        app.setBloodGroup(request.getBloodGroup());
        app.setNationality(request.getNationality());
        app.setReligion(request.getReligion());
        app.setCategory(request.getCategory());
        app.setPreviousSchoolName(request.getPreviousSchoolName());
        app.setPreviousSchoolClass(request.getPreviousSchoolClass());
        app.setPreviousSchoolPercentage(request.getPreviousSchoolPercentage());
        app.setPhone(request.getPhone());
        app.setEmail(request.getEmail());
        app.setFatherName(request.getFatherName());
        app.setFatherPhone(request.getFatherPhone());
        app.setMotherName(request.getMotherName());
        app.setMotherPhone(request.getMotherPhone());
        app.setGuardianName(request.getGuardianName());
        app.setGuardianPhone(request.getGuardianPhone());
        app.setGuardianRelation(request.getGuardianRelation());
        app.setAddressLine1(request.getAddressLine1());
        app.setAddressLine2(request.getAddressLine2());
        app.setCity(request.getCity());
        app.setState(request.getState());
        app.setPincode(request.getPincode());
        app.setCountry(request.getCountry());
        app.setNotes(request.getRemarks());
        app.setUpdatedBy(updatedBy != null ? updatedBy : "system");

        AdmissionApplication saved = applicationRepository.save(app);
        return mapToResponse(saved);
    }

    @Transactional
    public AdmissionApplicationResponse submitApplication(Long id, String submittedBy) {
        String tenantId = TenantContext.getTenantId();
        AdmissionApplication app = applicationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission application not found"));

        if (app.getStatus() != AdmissionStatus.DRAFT && app.getStatus() != AdmissionStatus.DOCUMENTS_REQUIRED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only DRAFT or DOCUMENTS_REQUIRED applications can be submitted");
        }

        app.setStatus(AdmissionStatus.SUBMITTED);
        app.setSubmittedAt(LocalDateTime.now());
        app.setUpdatedBy(submittedBy);

        AdmissionApplication saved = applicationRepository.save(app);
        log.info("Submitted admission application id={} by {}", id, submittedBy);
        return mapToResponse(saved);
    }

    @Transactional
    public AdmissionApplicationResponse startReview(Long id, String reviewer) {
        String tenantId = TenantContext.getTenantId();
        AdmissionApplication app = applicationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission application not found"));

        if (app.getStatus() != AdmissionStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application must be in SUBMITTED state to review");
        }

        app.setStatus(AdmissionStatus.UNDER_REVIEW);
        app.setReviewedBy(reviewer);
        app.setReviewedAt(LocalDateTime.now());
        app.setUpdatedBy(reviewer);

        AdmissionApplication saved = applicationRepository.save(app);
        return mapToResponse(saved);
    }

    @Transactional
    public AdmissionApplicationResponse requestDocuments(Long id, String reviewNotes, String reviewer) {
        String tenantId = TenantContext.getTenantId();
        AdmissionApplication app = applicationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission application not found"));

        app.setStatus(AdmissionStatus.DOCUMENTS_REQUIRED);
        app.setReviewNotes(reviewNotes);
        app.setReviewedBy(reviewer);
        app.setReviewedAt(LocalDateTime.now());
        app.setUpdatedBy(reviewer);

        AdmissionApplication saved = applicationRepository.save(app);
        return mapToResponse(saved);
    }

    @Transactional
    public AdmissionApplicationResponse approveApplication(Long id, String approver, String reviewNotes) {
        String tenantId = TenantContext.getTenantId();
        AdmissionApplication app = applicationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission application not found"));

        // Maker-checker rule: A user must not approve their own admission application
        if (approver != null && approver.equalsIgnoreCase(app.getCreatedBy())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Maker-Checker violation: You cannot approve an admission application that you created.");
        }

        if (app.getStatus() != AdmissionStatus.DOCUMENT_VERIFIED && app.getStatus() != AdmissionStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Application must be in DOCUMENT_VERIFIED or UNDER_REVIEW state to approve");
        }

        app.setStatus(AdmissionStatus.APPROVED);
        app.setApprovedBy(approver);
        app.setApprovedAt(LocalDateTime.now());
        if (reviewNotes != null && !reviewNotes.isBlank()) {
            app.setReviewNotes(reviewNotes);
        }
        app.setUpdatedBy(approver);

        AdmissionApplication saved = applicationRepository.save(app);
        log.info("Application id={} approved by {} for tenant {}", id, approver, tenantId);
        return mapToResponse(saved);
    }

    @Transactional
    public AdmissionApplicationResponse rejectApplication(Long id, String rejector, String reason) {
        String tenantId = TenantContext.getTenantId();
        AdmissionApplication app = applicationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission application not found"));

        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejection reason is mandatory");
        }

        app.setStatus(AdmissionStatus.REJECTED);
        app.setRejectedBy(rejector);
        app.setRejectedAt(LocalDateTime.now());
        app.setRejectionReason(reason.trim());
        app.setUpdatedBy(rejector);

        AdmissionApplication saved = applicationRepository.save(app);
        log.info("Application id={} rejected by {} with reason: {}", id, rejector, reason);
        return mapToResponse(saved);
    }

    @Transactional
    public AdmissionApplicationResponse waitlistApplication(Long id, String actor, String notes) {
        String tenantId = TenantContext.getTenantId();
        AdmissionApplication app = applicationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission application not found"));

        app.setStatus(AdmissionStatus.WAITLISTED);
        if (notes != null) app.setReviewNotes(notes);
        app.setUpdatedBy(actor);

        AdmissionApplication saved = applicationRepository.save(app);
        return mapToResponse(saved);
    }

    @Transactional
    public AdmissionApplicationResponse cancelApplication(Long id, String actor, String reason) {
        String tenantId = TenantContext.getTenantId();
        AdmissionApplication app = applicationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission application not found"));

        if (app.getStatus() == AdmissionStatus.CONVERTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel an already CONVERTED application");
        }

        app.setStatus(AdmissionStatus.CANCELLED);
        if (reason != null) app.setRejectionReason(reason);
        app.setUpdatedBy(actor);

        AdmissionApplication saved = applicationRepository.save(app);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public AdmissionApplicationResponse getApplication(Long id) {
        String tenantId = TenantContext.getTenantId();
        AdmissionApplication app = applicationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admission application not found"));
        return mapToResponse(app);
    }

    @Transactional(readOnly = true)
    public Page<AdmissionApplicationResponse> listApplications(AdmissionStatus status,
                                                               String applyingClass,
                                                               Long sessionId,
                                                               String query,
                                                               Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        Page<AdmissionApplication> page = applicationRepository.searchApplications(
                tenantId, status, applyingClass, sessionId, query, pageable);
        return page.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public AdmissionMetricsDto getMetrics(Long sessionId) {
        String tenantId = TenantContext.getTenantId();
        List<AdmissionApplication> all = applicationRepository.findAllByTenantId(tenantId);
        if (sessionId != null) {
            all = all.stream().filter(a -> sessionId.equals(a.getAcademicSessionId())).collect(Collectors.toList());
        }

        return AdmissionMetricsDto.builder()
                .totalApplications(all.size())
                .draftCount(all.stream().filter(a -> a.getStatus() == AdmissionStatus.DRAFT).count())
                .submittedCount(all.stream().filter(a -> a.getStatus() == AdmissionStatus.SUBMITTED).count())
                .underReviewCount(all.stream().filter(a -> a.getStatus() == AdmissionStatus.UNDER_REVIEW).count())
                .documentsRequiredCount(all.stream().filter(a -> a.getStatus() == AdmissionStatus.DOCUMENTS_REQUIRED).count())
                .documentVerifiedCount(all.stream().filter(a -> a.getStatus() == AdmissionStatus.DOCUMENT_VERIFIED).count())
                .approvedCount(all.stream().filter(a -> a.getStatus() == AdmissionStatus.APPROVED).count())
                .convertedCount(all.stream().filter(a -> a.getStatus() == AdmissionStatus.CONVERTED).count())
                .rejectedCount(all.stream().filter(a -> a.getStatus() == AdmissionStatus.REJECTED).count())
                .waitlistedCount(all.stream().filter(a -> a.getStatus() == AdmissionStatus.WAITLISTED).count())
                .cancelledCount(all.stream().filter(a -> a.getStatus() == AdmissionStatus.CANCELLED).count())
                .build();
    }

    public AdmissionApplicationResponse mapToResponse(AdmissionApplication app) {
        if (app == null) return null;

        List<AdmissionDocumentResponse> docs = null;
        if (app.getId() != null) {
            docs = documentRepository.findByApplicationIdAndTenantId(app.getId(), app.getTenantId()).stream()
                    .map(documentService::mapToResponse)
                    .collect(Collectors.toList());
        }

        String fullName = (app.getApplicantFirstName() + " " + (app.getApplicantLastName() != null ? app.getApplicantLastName() : "")).trim();

        return AdmissionApplicationResponse.builder()
                .id(app.getId())
                .tenantId(app.getTenantId())
                .applicationNumber(app.getApplicationNumber())
                .academicSessionId(app.getAcademicSessionId())
                .applyingClass(app.getApplyingClass())
                .stream(app.getStream())
                .applicantFirstName(app.getApplicantFirstName())
                .applicantLastName(app.getApplicantLastName())
                .applicantFullName(fullName)
                .dateOfBirth(app.getDateOfBirth())
                .gender(app.getGender())
                .bloodGroup(app.getBloodGroup())
                .nationality(app.getNationality())
                .religion(app.getReligion())
                .category(app.getCategory())
                .previousSchoolName(app.getPreviousSchoolName())
                .previousClass(app.getPreviousClass())
                .previousPercentage(app.getPreviousPercentage())
                .primaryContactName(app.getPrimaryContactName())
                .primaryContactPhone(app.getPrimaryContactPhone())
                .primaryContactEmail(app.getPrimaryContactEmail())
                .primaryContactRelationship(app.getPrimaryContactRelationship())
                .addressLine1(app.getAddressLine1())
                .addressLine2(app.getAddressLine2())
                .city(app.getCity())
                .state(app.getState())
                .postalCode(app.getPostalCode())
                .country(app.getCountry())
                .status(app.getStatus())
                .source(app.getSource())
                .remarks(app.getRemarks())
                .reviewNotes(app.getReviewNotes())
                .rejectionReason(app.getRejectionReason())
                .studentId(app.getStudentId())
                .admissionNumber(app.getAdmissionNumber())
                .submittedAt(app.getSubmittedAt())
                .reviewedAt(app.getReviewedAt())
                .reviewedBy(app.getReviewedBy())
                .approvedAt(app.getApprovedAt())
                .approvedBy(app.getApprovedBy())
                .convertedAt(app.getConvertedAt())
                .convertedBy(app.getConvertedBy())
                .rejectedAt(app.getRejectedAt())
                .rejectedBy(app.getRejectedBy())
                .createdBy(app.getCreatedBy())
                .updatedBy(app.getUpdatedBy())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .documents(docs)
                .build();
    }
}
