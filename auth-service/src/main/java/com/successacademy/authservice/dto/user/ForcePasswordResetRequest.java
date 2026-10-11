package com.successacademy.authservice.dto.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForcePasswordResetRequest {

    private String newPassword;
    private boolean mustChangePassword;
}
