package com.successacademy.studentservice.controller;

import com.successacademy.studentservice.dto.*;
import com.successacademy.studentservice.model.AdmissionDocument;
import com.successacademy.studentservice.model.AdmissionStatus;
import com.successacademy.studentservice.model.DocumentType;
import com.successacademy.studentservice.security.SecurityContextUtil;
import com.successacademy.studentservice.service.AdmissionApplicationService;
import com.successacademy.studentservice.service.AdmissionConversionService;
import com.successacademy.studentservice.service.AdmissionDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admissions")
@RequiredArgsConstructor
@Slf4j
public class AdmissionController {

    private final AdmissionApplicationService applicationService;
    private final AdmissionConversionService conversionService;
    private final AdmissionDocumentService documentService;
    private final SecurityContextUtil securityContextUtil;

    /**
     * Create application (Staff/Admin or Internal)
     */
    @PostMapping({"", "/applications"})
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionApplicationResponse createApplication(@Valid @RequestBody AdmissionApplicationRequest request,
                                                           @RequestParam(defaultValue = "false") boolean submit,
                                                           HttpServletRequest httpRequest) {
        String actor = securityContextUtil.getCurrentUsername(httpRequest);
        return applicationService.createApplication(request, actor, submit);
    }

    /**
     * Public application submission (e.g. prospective parent/student portal)
     */
    @PostMapping("/public")
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionApplicationResponse submitPublicApplication(@Valid @RequestBody AdmissionApplicationRequest request) {
        return applicationService.createApplication(request, "public-portal", true);
    }

    /**
     * Update draft application
     */
    @PutMapping("/{id}")
    public AdmissionApplicationResponse updateApplication(@PathVariable Long id,
                                                           @Valid @RequestBody AdmissionApplicationRequest request,
                                                           HttpServletRequest httpRequest) {
        String actor = securityContextUtil.getCurrentUsername(httpRequest);
        return applicationService.updateApplication(id, request, actor);
    }

    /**
     * Submit draft application
     */
    @PostMapping("/{id}/submit")
    public AdmissionApplicationResponse submitApplication(@PathVariable Long id, HttpServletRequest httpRequest) {
        String actor = securityContextUtil.getCurrentUsername(httpRequest);
        return applicationService.submitApplication(id, actor);
    }

    /**
     * Move application to UNDER_REVIEW
     */
    @PostMapping("/{id}/review")
    public AdmissionApplicationResponse startReview(@PathVariable Long id, HttpServletRequest httpRequest) {
        securityContextUtil.requireAdmin(httpRequest);
        String reviewer = securityContextUtil.getCurrentUsername(httpRequest);
        return applicationService.startReview(id, reviewer);
    }

    /**
     * Request additional documents
     */
    @PostMapping("/{id}/request-documents")
    public AdmissionApplicationResponse requestDocuments(@PathVariable Long id,
                                                          @RequestBody(required = false) Map<String, String> body,
                                                          HttpServletRequest httpRequest) {
        securityContextUtil.requireAdmin(httpRequest);
        String reviewer = securityContextUtil.getCurrentUsername(httpRequest);
        String notes = (body != null) ? body.get("reviewNotes") : null;
        return applicationService.requestDocuments(id, notes, reviewer);
    }

    /**
     * Approve admission application (Maker-Checker enforced)
     */
    @PostMapping("/{id}/approve")
    public AdmissionApplicationResponse approveApplication(@PathVariable Long id,
                                                           @RequestBody(required = false) Map<String, String> body,
                                                           HttpServletRequest httpRequest) {
        securityContextUtil.requireAdmin(httpRequest);
        String approver = securityContextUtil.getCurrentUsername(httpRequest);
        String notes = (body != null) ? body.get("reviewNotes") : null;
        return applicationService.approveApplication(id, approver, notes);
    }

    /**
     * Reject admission application
     */
    @PostMapping("/{id}/reject")
    public AdmissionApplicationResponse rejectApplication(@PathVariable Long id,
                                                          @RequestBody Map<String, String> body,
                                                          HttpServletRequest httpRequest) {
        securityContextUtil.requireAdmin(httpRequest);
        String rejector = securityContextUtil.getCurrentUsername(httpRequest);
        String reason = body.get("reason");
        return applicationService.rejectApplication(id, rejector, reason);
    }

    /**
     * Waitlist admission application
     */
    @PostMapping("/{id}/waitlist")
    public AdmissionApplicationResponse waitlistApplication(@PathVariable Long id,
                                                            @RequestBody(required = false) Map<String, String> body,
                                                            HttpServletRequest httpRequest) {
        securityContextUtil.requireAdmin(httpRequest);
        String actor = securityContextUtil.getCurrentUsername(httpRequest);
        String notes = (body != null) ? body.get("notes") : null;
        return applicationService.waitlistApplication(id, actor, notes);
    }

    /**
     * Cancel admission application
     */
    @PostMapping("/{id}/cancel")
    public AdmissionApplicationResponse cancelApplication(@PathVariable Long id,
                                                          @RequestBody(required = false) Map<String, String> body,
                                                          HttpServletRequest httpRequest) {
        String actor = securityContextUtil.getCurrentUsername(httpRequest);
        String reason = (body != null) ? body.get("reason") : null;
        return applicationService.cancelApplication(id, actor, reason);
    }

    /**
     * Convert approved application to Student (Idempotent + Transactional)
     */
    @PostMapping("/{id}/convert")
    public StudentResponse convertToStudent(@PathVariable Long id,
                                            @RequestBody(required = false) AdmissionConversionRequest request,
                                            HttpServletRequest httpRequest) {
        securityContextUtil.requireAdmin(httpRequest);
        String actor = securityContextUtil.getCurrentUsername(httpRequest);
        return conversionService.convertToStudent(id, request, actor);
    }

    /**
     * Get single application
     */
    @GetMapping("/{id}")
    public AdmissionApplicationResponse getApplication(@PathVariable Long id) {
        return applicationService.getApplication(id);
    }

    /**
     * List / Search applications with server-side pagination & filtering
     */
    @GetMapping({"", "/applications"})
    public Page<AdmissionApplicationResponse> listApplications(
            @RequestParam(required = false) AdmissionStatus status,
            @RequestParam(required = false) String applyingClass,
            @RequestParam(required = false) Long academicSessionId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        String[] sortParts = sort.split(",");
        String sortProp = sortParts[0];
        Sort.Direction direction = (sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1]))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, sortProp));
        return applicationService.listApplications(status, applyingClass, academicSessionId, search, pageable);
    }

    /**
     * Metrics overview for dashboard
     */
    @GetMapping("/metrics")
    public AdmissionMetricsDto getMetrics(@RequestParam(required = false) Long academicSessionId) {
        return applicationService.getMetrics(academicSessionId);
    }

    /**
     * Upload admission document
     */
    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AdmissionDocumentResponse uploadDocument(
            @PathVariable Long id,
            @RequestParam("type") DocumentType documentType,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        String actor = securityContextUtil.getCurrentUsername(httpRequest);
        return documentService.uploadDocument(id, documentType, file, actor);
    }

    /**
     * Get documents for application
     */
    @GetMapping("/{id}/documents")
    public List<AdmissionDocumentResponse> getDocuments(@PathVariable Long id) {
        return documentService.getDocumentsForApplication(id);
    }

    /**
     * Download / Stream admission document
     */
    @GetMapping("/documents/{docId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long docId) {
        AdmissionDocument doc = documentService.getDocument(docId);
        Resource resource = documentService.loadDocumentAsResource(docId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getOriginalFileName() + "\"")
                .body(resource);
    }

    /**
     * Verify or reject admission document
     */
    @PutMapping("/documents/{docId}/verify")
    public AdmissionDocumentResponse verifyDocument(
            @PathVariable Long docId,
            @Valid @RequestBody DocumentVerificationRequest request,
            HttpServletRequest httpRequest) {
        securityContextUtil.requireAdmin(httpRequest);
        String actor = securityContextUtil.getCurrentUsername(httpRequest);
        return documentService.verifyDocument(docId, request, actor);
    }
}
