package com.successacademy.studentservice.dto;

import com.successacademy.studentservice.model.GuardianRelationship;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGuardianDto {
    private Long id;
    private Long studentId;
    private Long guardianId;
    private GuardianResponse guardian;
    private GuardianRelationship relationshipType;
    private Boolean isPrimary;
    private Boolean isEmergencyContact;
    private Boolean canPickup;
    private Boolean canReceiveNotifications;
    private Boolean canAccessPortal;
    private String status;
    private LocalDateTime createdAt;
}
