package com.successacademy.authservice.dto.user;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationResponseDto {

    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String status;
    private LocalDateTime expiresAt;
    private String rawToken; // One-time token returned only at creation time to caller (never persisted)
    private LocalDateTime createdAt;
}
