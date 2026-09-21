package com.successacademy.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummary {
    private Long id;
    private String username;
    private String email;
    private String role;
    private Long studentId;
    private Long teacherId;
}
