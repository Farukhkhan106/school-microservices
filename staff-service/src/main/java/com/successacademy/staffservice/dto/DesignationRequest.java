package com.successacademy.staffservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignationRequest {

    private Long departmentId;
    private String code;
    private String name;
    private String description;
    private String status; // ACTIVE, INACTIVE
}
