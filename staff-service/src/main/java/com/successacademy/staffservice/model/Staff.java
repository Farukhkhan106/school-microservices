package com.successacademy.staffservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String staffCode; // e.g. STF-0001

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(length = 100)
    private String middleName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(length = 500)
    private String photoUrl;

    @Column(length = 20)
    private String gender;

    private LocalDate dateOfBirth;

    // Contact info — optional!
    @Column(length = 25)
    private String phone;

    @Column(length = 25)
    private String alternatePhone;

    @Column(length = 150)
    private String email;

    // Address
    @Column(length = 300)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 20)
    private String pincode;

    // Emergency Contact
    @Column(length = 150)
    private String emergencyContactName;

    @Column(length = 50)
    private String emergencyContactRelation;

    @Column(length = 25)
    private String emergencyContactPhone;

    // Organization references
    private Long departmentId;
    private Long designationId;

    @Column(length = 50)
    private String category;

    @Column(length = 100)
    private String designation;

    @Column(length = 100)
    private String department;

    // Employment
    @Column(length = 30)
    private String employmentType; // FULL_TIME, PART_TIME, CONTRACT, TEMPORARY, OUTSOURCED, INTERN

    private LocalDate joiningDate;
    private LocalDate lastWorkingDate;

    private Long reportingManagerStaffId;

    // Employment lifecycle status
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, ON_NOTICE, SUSPENDED, RESIGNED, TERMINATED, RETIRED

    // System access lifecycle
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String systemAccessStatus = "NOT_PROVISIONED"; // NOT_PROVISIONED, INVITED, ACTIVE, LOCKED, DISABLED

    // Optional link to auth-service users.id
    private Long userId;

    // Access profile / role template
    @Column(length = 50)
    private String accessProfile; // STAFF_BASIC, RECEPTION, ACCOUNTING, TRANSPORT, LIBRARY, SECURITY, HOUSEKEEPING, STAFF_MANAGER

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
        if (this.systemAccessStatus == null) this.systemAccessStatus = "NOT_PROVISIONED";
        if (this.employmentType == null) this.employmentType = "FULL_TIME";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
