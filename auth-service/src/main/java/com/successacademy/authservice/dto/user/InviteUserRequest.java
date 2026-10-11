package com.successacademy.authservice.dto.user;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteUserRequest {

    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String phone;
    private Long roleId;
    private String roleCode;
    private String userType;
    private Long studentId;
    private Long teacherId;
    private Long staffId;
}
