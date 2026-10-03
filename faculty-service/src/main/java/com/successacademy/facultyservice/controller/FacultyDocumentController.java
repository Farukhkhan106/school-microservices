package com.successacademy.facultyservice.controller;

import com.successacademy.facultyservice.dto.FacultyDocumentResponse;
import com.successacademy.facultyservice.security.SecurityContextUtil;
import com.successacademy.facultyservice.service.FacultyDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/faculty/documents")
@RequiredArgsConstructor
@Slf4j
public class FacultyDocumentController {

    private final FacultyDocumentService documentService;
    private final SecurityContextUtil securityUtil;

    @PostMapping("/faculty/{facultyId}")
    public ResponseEntity<FacultyDocumentResponse> uploadDocument(
            @PathVariable Long facultyId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", defaultValue = "OTHER") String documentType,
            @RequestParam(value = "documentName", required = false) String documentName,
            @RequestParam(value = "documentNumber", required = false) String documentNumber,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        Long actorUserId = securityUtil.getCurrentUserId(req);
        FacultyDocumentResponse response = documentService.uploadDocument(
                facultyId, documentType, documentName, documentNumber, file, actorUserId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/faculty/{facultyId}")
    public ResponseEntity<List<FacultyDocumentResponse>> getDocumentsForFaculty(
            @PathVariable Long facultyId,
            HttpServletRequest req
    ) {
        securityUtil.requireAdmin(req);
        List<FacultyDocumentResponse> list = documentService.getDocumentsForFaculty(facultyId);
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
