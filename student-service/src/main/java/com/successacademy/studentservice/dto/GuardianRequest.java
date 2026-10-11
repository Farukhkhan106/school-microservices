package com.successacademy.studentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuardianRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    private String lastName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String alternatePhone;
    private String email;
    private String address;
    private String occupation;
    private String relationshipMetadata;
}
