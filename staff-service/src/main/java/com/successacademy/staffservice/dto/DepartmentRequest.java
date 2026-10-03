package com.successacademy.staffservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentRequest {

    private String code;
    private String name;
    private String description;
    private String status; // ACTIVE, INACTIVE
}
