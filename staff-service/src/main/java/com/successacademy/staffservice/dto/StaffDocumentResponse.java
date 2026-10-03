package com.successacademy.staffservice.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffDocumentResponse {

    private Long id;
    private Long staffId;
    private String documentType;
    private String documentNumber;
    private String fileUrl;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String status; // ACTIVE, EXPIRED, ARCHIVED
    private String expiryStatus; // "VALID", "EXPIRING_SOON", "EXPIRED", "NO_EXPIRY"
    private Long daysUntilExpiry;
    private Long uploadedBy;
    private LocalDateTime createdAt;
}
