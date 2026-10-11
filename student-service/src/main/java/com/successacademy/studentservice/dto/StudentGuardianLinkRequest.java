package com.successacademy.studentservice.dto;

import com.successacademy.studentservice.model.GuardianRelationship;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGuardianLinkRequest {
    private Long guardianId;
    private GuardianRequest inlineGuardian; // Optional if creating a new guardian on the fly

    @NotNull(message = "Relationship type is required")
    private GuardianRelationship relationshipType;

    private Boolean isPrimary;
    private Boolean isEmergencyContact;
    private Boolean canPickup;
    private Boolean canReceiveNotifications;
    private Boolean canAccessPortal;
}
