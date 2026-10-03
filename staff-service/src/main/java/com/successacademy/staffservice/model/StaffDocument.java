package com.successacademy.staffservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long staffId;

    // IDENTITY_PROOF, ADDRESS_PROOF, JOINING_LETTER, CONTRACT, QUALIFICATION, POLICE_VERIFICATION, DRIVING_LICENSE, OTHER
    @Column(nullable = false, length = 50)
    private String documentType;

    @Column(length = 100)
    private String documentNumber;

    @Column(nullable = false, length = 500)
    private String fileUrl;

    private LocalDate issueDate;
    private LocalDate expiryDate;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, ARCHIVED

    private Long uploadedBy; // actorUserId

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
    }
}
