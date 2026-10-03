package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.StaffDocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface StaffDocumentService {

    StaffDocumentResponse uploadDocument(
            Long staffId,
            String documentType,
            String documentNumber,
            LocalDate issueDate,
            LocalDate expiryDate,
            MultipartFile file,
            Long actorUserId
    );

    List<StaffDocumentResponse> getDocumentsForStaff(Long staffId);

    void deleteDocument(Long id, Long actorUserId);
}
