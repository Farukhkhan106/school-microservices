package com.successacademy.communicationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastRequest {

    private String title;

    @NotBlank(message = "Broadcast content cannot be blank")
    @Size(max = 4000, message = "Broadcast content cannot exceed 4000 characters")
    private String content;

    // ALL, TEACHER, STUDENT
    private String targetRole = "ALL";

    // Optional class filter
    private String targetClass;
    private String targetSection;
}
