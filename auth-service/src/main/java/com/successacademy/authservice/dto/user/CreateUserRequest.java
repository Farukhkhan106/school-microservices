package com.successacademy.authservice.dto.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    private String username;
    private String email;
    private String phone;
    private String password;
    private String firstName;
    private String lastName;
    private String displayName;
    private String userType; // PLATFORM_ADMIN, TENANT_ADMIN, ADMIN_DELEGATE, PRINCIPAL, TEACHER, STAFF, SUPPORT, STUDENT
    private Long roleId;     // Primary role to assign
    private String roleCode; // Alternative to roleId

    private Long studentId;
    private Long teacherId;
    private Long staffId;

    private boolean mustChangePassword;
    private boolean sendInvitation;
}
