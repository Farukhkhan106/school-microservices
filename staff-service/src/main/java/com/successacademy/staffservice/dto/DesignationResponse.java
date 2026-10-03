package com.successacademy.staffservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignationResponse {

    private Long id;
    private Long departmentId;
    private String departmentName;
    private String code;
    private String name;
    private String description;
    private String status;
    private long staffCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
