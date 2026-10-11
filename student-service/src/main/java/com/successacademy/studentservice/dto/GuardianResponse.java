package com.successacademy.studentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianResponse {
    private Long id;
    private String tenantId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String displayName;
    private String phone;
    private String alternatePhone;
    private String email;
    private String address;
    private String occupation;
    private String relationshipMetadata;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
