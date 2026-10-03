package com.successacademy.academicservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionResponse {
    private Long id;
    private String sessionCode;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;

    @JsonProperty("isActive")
    private boolean isActive;

    private String description;
    private LocalDateTime createdAt;

    @JsonProperty("active")
    public boolean getActive() {
        return this.isActive;
    }
}
