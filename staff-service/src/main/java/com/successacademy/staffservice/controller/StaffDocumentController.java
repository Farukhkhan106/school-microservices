package com.successacademy.staffservice.controller;

import com.successacademy.staffservice.dto.StaffDocumentResponse;
import com.successacademy.staffservice.security.SecurityContextUtil;
import com.successacademy.staffservice.service.StaffDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Slf4j
public class StaffDocumentController {

    private final StaffDocumentService documentService;
    private final SecurityContextUtil securityUtil;

    @PostMapping("/staff/{staffId}")
    public ResponseEntity<StaffDocumentResponse> uploadDocument(
            @PathVariable Long staffId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            @RequestParam(value = "issueDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate,
            @RequestParam(value = "expiryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        StaffDocumentResponse response = documentService.uploadDocument(
                staffId, documentType, documentNumber, issueDate, expiryDate, file, actorUserId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<StaffDocumentResponse>> getDocumentsForStaff(
            @PathVariable Long staffId,
            HttpServletRequest req
    ) {
        securityUtil.requireSelfOrAdmin(req, staffId);
        List<StaffDocumentResponse> list = documentService.getDocumentsForStaff(staffId);
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteDocument(
            @PathVariable Long id,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        documentService.deleteDocument(id, actorUserId);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
    }
}
