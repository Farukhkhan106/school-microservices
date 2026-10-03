package com.successacademy.staffservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String status;
    private long staffCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
