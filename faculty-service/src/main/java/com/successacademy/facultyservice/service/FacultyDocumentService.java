package com.successacademy.facultyservice.service;

import com.successacademy.facultyservice.dto.FacultyDocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FacultyDocumentService {

    FacultyDocumentResponse uploadDocument(Long facultyId, String documentType, String documentName,
                                          String documentNumber, MultipartFile file, Long actorUserId);

    List<FacultyDocumentResponse> getDocumentsForFaculty(Long facultyId);

    void deleteDocument(Long documentId, Long actorUserId);
}
