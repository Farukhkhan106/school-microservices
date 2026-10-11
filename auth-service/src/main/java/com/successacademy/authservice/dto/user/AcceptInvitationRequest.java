package com.successacademy.authservice.dto.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptInvitationRequest {

    private String token;
    private String newPassword;
}
