package com.successacademy.authservice.dto.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String displayName;
    private String userType;

    private Long studentId;
    private Long teacherId;
    private Long staffId;
}
