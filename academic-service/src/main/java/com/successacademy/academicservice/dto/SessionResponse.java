package com.successacademy.academicservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.successacademy.academicservice.model.SessionStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionResponse {
    private Long id;
    private String tenantId;
    private String sessionCode;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private SessionStatus status;

    @JsonProperty("isActive")
    private boolean isActive;

    private String description;
    private Long studentEnrollmentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonProperty("active")
    public boolean getActive() {
        return this.isActive;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isActive() {
        return this.isActive;
    }
}
